package eu.wdaqua.qanary.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.exceptions.ParallelStreamsFailedException;
import eu.wdaqua.qanary.exceptions.ParallelStreamsFailedException.StreamFailure;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the execution of process definitions with the real
 * {@link QanaryConfigurator} calling Qanary components over HTTP; the
 * components are served by a local HTTP server that handles requests
 * concurrently and records when each component starts and ends.
 * <p>
 * Parallelism is shown with latches, not with timing: a component that waits
 * for another one to start can only finish if both run at the same time.
 */
class QanaryProcessExecutorTest {

	private static final URI ENDPOINT = URI.create("urn:test:qanary-triplestore");
	private static final URI GRAPH = URI.create("urn:graph:parallel-test");
	private static final long TIMEOUT_SECONDS = 20;

	/** what a component does when it is called: returns the HTTP status of its response */
	@FunctionalInterface
	private interface Behaviour {
		int run() throws InterruptedException;
	}

	private HttpServer server;
	private final List<String> events = Collections.synchronizedList(new ArrayList<>());
	private final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, Behaviour> behaviours = new ConcurrentHashMap<>();
	private final Map<String, CountDownLatch> started = new ConcurrentHashMap<>();
	private final Set<String> registered = ConcurrentHashMap.newKeySet();
	private final List<String[]> unavailable = Collections.synchronizedList(new ArrayList<>());

	private QanaryConfigurator configurator;
	private QanaryProcessExecutor executor;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		server.createContext("/", this::serveComponent);
		// concurrent requests have to be served concurrently (the default executor is a single thread)
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		server.start();
		registered.addAll(List.of("a", "b", "c", "d", "e", "f"));

		configurator = spy(new QanaryConfigurator(new RestTemplate(), List.of(), "http://127.0.0.1", 0, null,
				ENDPOINT, null));
		executor = new QanaryProcessExecutor(configurator, this::availableComponents,
				(component, stream) -> unavailable.add(new String[] { component, stream }));
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	private List<QanaryComponent> availableComponents(List<String> names) {
		String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		return names.stream().filter(registered::contains)
				.map(name -> new QanaryComponent(name, baseUrl + "/" + name, true)).toList();
	}

	/**
	 * serves {@code POST /{component}/annotatequestion} like a Qanary component:
	 * returns the received message
	 */
	private void serveComponent(HttpExchange exchange) throws IOException {
		String component = exchange.getRequestURI().getPath().split("/")[1];
		byte[] message = exchange.getRequestBody().readAllBytes();
		receivedMessages.add(component + " " + new String(message, StandardCharsets.UTF_8));
		events.add("start " + component);
		latch(component).countDown();
		int status;
		try {
			status = behaviours.getOrDefault(component, () -> 200).run();
		} catch (InterruptedException e) {
			status = 503;
		}
		events.add("end " + component);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, message.length);
		exchange.getResponseBody().write(message);
		exchange.close();
	}

	private CountDownLatch latch(String component) {
		return started.computeIfAbsent(component, c -> new CountDownLatch(1));
	}

	/** the component returns HTTP 200 only if the other component starts while it is running */
	private Behaviour runsParallelTo(String other) {
		return () -> latch(other).await(TIMEOUT_SECONDS, TimeUnit.SECONDS) ? 200 : 504;
	}

	private int position(String event) {
		int position = events.indexOf(event);
		assertTrue(position >= 0, event + " did not happen: " + events);
		return position;
	}

	private QanaryQuestionAnsweringFinished execute(String componentlist) throws Exception {
		return executor.execute(definition(componentlist), new QanaryMessage(ENDPOINT, GRAPH));
	}

	private static QanaryProcessDefinition definition(String componentlist) {
		return JsonMapper.builder().build().readValue(componentlist, QanaryProcessDefinition.class);
	}

	/** the component names of the callServices invocations, in the order they started */
	private List<List<String>> callServicesInvocations() {
		return mockingDetails(configurator).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("callServices"))
				.map(invocation -> invocation.<List<QanaryComponent>>getArgument(0).stream()
						.map(QanaryComponent::getName).toList())
				.toList();
	}

	private static List<String> componentNames(QanaryQuestionAnsweringFinished protocol) {
		return protocol.getExtendedProtocol().stream()
				.map(QanaryQuestionAnsweringFinished.ComponentExecutionLog::getComponentName).toList();
	}

	// ---- sequential process definitions --------------------------------------------------

	@Test
	void aSequentialDefinitionCallsAllComponentsInOneSequence() throws Exception {
		QanaryQuestionAnsweringFinished protocol = execute("[\"a\", \"b\", \"c\"]");

		assertEquals(List.of("start a", "end a", "start b", "end b", "start c", "end c"), events);
		// exactly as before the process definitions existed: one call of callServices
		assertEquals(List.of(List.of("a", "b", "c")), callServicesInvocations());
		assertEquals(List.of("a", "b", "c"), componentNames(protocol));
		assertEquals(GRAPH, protocol.getQanaryMessage().getInGraph());
		assertTrue(protocol.getDurationInMilliseconds() >= 0);
	}

	@Test
	void unavailableComponentsAreReportedAndSkipped() throws Exception {
		registered.remove("b");

		QanaryQuestionAnsweringFinished protocol = execute("[\"a\", \"b\", {\"p1\": [\"b\", \"c\"], \"p2\": [\"d\"]}]");

		assertEquals(List.of("a", "c", "d"), componentNames(protocol));
		assertEquals(2, unavailable.size());
		assertEquals(List.of("b", "null"), List.of(unavailable.get(0)[0], String.valueOf(unavailable.get(0)[1])));
		assertEquals(List.of("b", "p1"), List.of(unavailable.get(1)));
	}

	@Test
	void ifNoComponentIsAvailableNoneIsCalled() throws Exception {
		registered.clear();

		QanaryQuestionAnsweringFinished protocol = execute("[\"a\", {\"p1\": [\"b\"]}]");

		assertTrue(events.isEmpty());
		assertTrue(callServicesInvocations().isEmpty());
		assertTrue(protocol.getExtendedProtocol().isEmpty());
		assertEquals(2, unavailable.size());
	}

	// ---- parallel blocks ------------------------------------------------------------------------

	@Test
	void theStreamsOfAParallelBlockRunAtTheSameTime() throws Exception {
		// b and c can only finish if they run at the same time
		behaviours.put("b", runsParallelTo("c"));
		behaviours.put("c", runsParallelTo("b"));

		QanaryQuestionAnsweringFinished protocol = execute("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]");

		assertEquals(10, events.size(), events.toString());
		// a before the parallel block
		assertTrue(position("end a") < position("start b"), events.toString());
		assertTrue(position("end a") < position("start c"), events.toString());
		// b parallel to c
		assertTrue(position("start b") < position("end c"), events.toString());
		assertTrue(position("start c") < position("end b"), events.toString());
		// d after c in stream x
		assertTrue(position("end c") < position("start d"), events.toString());
		// e after all streams
		assertTrue(position("end b") < position("start e"), events.toString());
		assertTrue(position("end d") < position("start e"), events.toString());

		// consecutive components are called together, each stream on its own
		List<List<String>> invocations = callServicesInvocations();
		assertEquals(4, invocations.size());
		assertEquals(List.of("a"), invocations.get(0));
		assertEquals(Set.of(List.of("b"), List.of("c", "d")), Set.copyOf(invocations.subList(1, 3)));
		assertEquals(List.of("e"), invocations.get(3));

		// the protocol lists the components in the order of the definition
		assertEquals(List.of("a", "b", "c", "d", "e"), componentNames(protocol));
		// every component received the message of the process
		assertEquals(5, receivedMessages.size());
		assertTrue(receivedMessages.stream().allMatch(message -> message.contains(GRAPH.toString())),
				receivedMessages.toString());
	}

	@Test
	void eachStreamRunsInItsOwnThreadNamedAfterTheStream() throws Exception {
		List<String> threads = Collections.synchronizedList(new ArrayList<>());
		doAnswer(invocation -> {
			threads.add(Thread.currentThread().getName());
			return invocation.callRealMethod();
		}).when(configurator).callServices(anyList(), any());

		execute("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\"]}]");

		assertEquals(3, threads.size());
		assertEquals(Thread.currentThread().getName(), threads.get(0));
		assertEquals(Set.of("qanary-stream-p1", "qanary-stream-x"), Set.copyOf(threads.subList(1, 3)));
	}

	@Test
	void nestedParallelBlocksRunAtTheSameTimeAsWell() throws Exception {
		behaviours.put("c", runsParallelTo("d"));
		behaviours.put("d", runsParallelTo("e"));
		behaviours.put("e", runsParallelTo("c"));

		QanaryQuestionAnsweringFinished protocol = execute(
				"[\"a\", {\"outer\": [\"b\", {\"inner1\": [\"c\"], \"inner2\": [\"d\"]}], \"other\": [\"e\"]}, \"f\"]");

		assertEquals(List.of("a", "b", "c", "d", "e", "f"), componentNames(protocol));
		assertTrue(position("end b") < position("start c"), events.toString());
		assertTrue(position("end b") < position("start d"), events.toString());
		assertTrue(position("end c") < position("start f"), events.toString());
		assertTrue(position("end d") < position("start f"), events.toString());
		assertTrue(position("end e") < position("start f"), events.toString());
	}

	// ---- failures -----------------------------------------------------------------------------------

	@Test
	void aFailingStreamIsNamedAndTheOtherStreamsAreCompleted() throws Exception {
		// c fails while b is still running: b is completed anyway
		behaviours.put("c", () -> 500);
		behaviours.put("b", () -> {
			latch("c").await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			Thread.sleep(100); // give the failure of c a head start
			return 200;
		});

		ParallelStreamsFailedException e = assertThrows(ParallelStreamsFailedException.class,
				() -> execute("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]"));

		assertEquals(1, e.getFailures().size());
		StreamFailure failure = e.getFailures().get(0);
		assertEquals("x", failure.stream());
		assertEquals("c", failure.component());
		assertInstanceOf(QanaryExceptionServiceCallNotOk.class, failure.cause());
		assertEquals("c", e.getComponentName());
		assertTrue(e.getMessage().startsWith("1 parallel stream(s) failed: stream \"x\", component \"c\": Call to "
				+ "Qanary component c was not performed correctly"), e.getMessage());
		assertFalse(e.getMessage().contains("\tat "), "no stack trace in the message: " + e.getMessage());
		assertEquals(1, e.getSuppressed().length);
		assertTrue(e.getDuration() >= 100, "the block ran until b had finished: " + e.getDuration());

		// b was completed, d (after c in its stream) and e (after the block) were not executed
		assertTrue(events.contains("end b"), events.toString());
		assertFalse(events.contains("start d"), events.toString());
		assertFalse(events.contains("start e"), events.toString());
	}

	@Test
	void allFailedStreamsAreReportedInTheOrderOfTheDefinition() throws Exception {
		// b in p1 fails after c in x has failed
		behaviours.put("c", () -> 500);
		behaviours.put("b", () -> {
			latch("c").await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			Thread.sleep(100);
			return 500;
		});

		ParallelStreamsFailedException e = assertThrows(ParallelStreamsFailedException.class,
				() -> execute("[{\"p1\": [\"b\"], \"x\": [\"c\"], \"ok\": [\"d\"]}]"));

		assertEquals(List.of("p1", "x"), e.getFailures().stream().map(StreamFailure::stream).toList());
		assertEquals(List.of("b", "c"), e.getFailures().stream().map(StreamFailure::component).toList());
		assertEquals("b", e.getComponentName());
		assertTrue(e.getMessage().startsWith("2 parallel stream(s) failed: stream \"p1\""), e.getMessage());
		assertTrue(events.contains("end d"), events.toString());
	}

	@Test
	void aFailureInANestedBlockNamesTheInnermostStream() throws Exception {
		behaviours.put("d", () -> 500);

		ParallelStreamsFailedException e = assertThrows(ParallelStreamsFailedException.class, () -> execute(
				"[{\"outer\": [\"b\", {\"inner1\": [\"c\"], \"inner2\": [\"d\"]}, \"f\"], \"other\": [\"e\"]}]"));

		assertEquals(1, e.getFailures().size());
		assertEquals("inner2", e.getFailures().get(0).stream());
		assertEquals("d", e.getFailures().get(0).component());
		assertTrue(events.contains("end c") && events.contains("end e"), events.toString());
		assertFalse(events.contains("start f"), events.toString());
	}

	@Test
	void aFailureInTheMainSequenceIsAPlainFailedCall() {
		behaviours.put("a", () -> 500);

		QanaryExceptionServiceCallNotOk e = assertThrows(QanaryExceptionServiceCallNotOk.class,
				() -> execute("[\"a\", {\"p1\": [\"b\"]}]"));

		assertFalse(e instanceof ParallelStreamsFailedException);
		assertEquals("a", e.getComponentName());
		assertFalse(events.contains("start b"), events.toString());
	}

	@Test
	void aStreamFailingWithoutAComponentCallIsReportedWithoutComponent() throws Exception {
		IllegalStateException failure = new IllegalStateException("triplestore not reachable");
		doThrow(failure).when(configurator)
				.callServices(argThat(components -> components.size() == 1 && components.get(0).getName().equals("c")),
						any());

		ParallelStreamsFailedException e = assertThrows(ParallelStreamsFailedException.class,
				() -> execute("[{\"p1\": [\"b\"], \"x\": [\"c\"]}]"));

		assertEquals("x", e.getFailures().get(0).stream());
		assertNull(e.getFailures().get(0).component());
		assertSame(failure, e.getFailures().get(0).cause());
		assertNull(e.getComponentName());
		assertTrue(e.getMessage().endsWith("stream \"x\": triplestore not reachable"), e.getMessage());
	}

	@Test
	void aFailureWithoutMessageIsNamedByItsClass() throws Exception {
		doThrow(new NullPointerException()).when(configurator)
				.callServices(argThat(components -> components.get(0).getName().equals("b")), any());

		ParallelStreamsFailedException e = assertThrows(ParallelStreamsFailedException.class,
				() -> execute("[{\"p1\": [\"b\"]}]"));

		assertTrue(e.getMessage().endsWith("stream \"p1\": java.lang.NullPointerException"), e.getMessage());
	}

	@Test
	void anErrorInAStreamIsNotTurnedIntoAFailedStream() throws Exception {
		AssertionError error = new AssertionError("broken");
		doThrow(error).when(configurator)
				.callServices(argThat(components -> components.get(0).getName().equals("b")), any());

		AssertionError thrown = assertThrows(AssertionError.class, () -> execute("[{\"p1\": [\"b\"], \"x\": [\"c\"]}]"));

		assertSame(error, thrown);
	}
}
