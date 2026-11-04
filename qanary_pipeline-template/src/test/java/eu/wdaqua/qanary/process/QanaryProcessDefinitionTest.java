package eu.wdaqua.qanary.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.wdaqua.qanary.exceptions.InvalidProcessDefinitionException;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.ComponentStep;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.NamedStream;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.ParallelStep;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the process definition, i.e., the {@code componentlist} of a JSON
 * request: reading it as Spring MVC does (Jackson 3), the sequential lists that
 * were possible before, parallel blocks, and the rejection of invalid
 * definitions.
 */
class QanaryProcessDefinitionTest {

	private final ObjectMapper mapper = JsonMapper.builder().build();

	private QanaryProcessDefinition read(String componentlist) {
		String request = "{\"question\": \"What is the capital of Saxony?\", \"componentlist\": " + componentlist + "}";
		return mapper.readValue(request, RequestQuestionAnsweringProcess.class).getProcessDefinition();
	}

	private static ComponentStep component(String name) {
		return new ComponentStep(name);
	}

	// ---- valid process definitions ----------------------------------------------------

	@Test
	void aListOfComponentNamesIsExecutedOneAfterAnother() {
		QanaryProcessDefinition process = read("[\"a\", \"b\"]");

		assertEquals(List.of(component("a"), component("b")), process.steps());
		assertTrue(process.isSequential());
		assertFalse(process.isEmpty());
		assertEquals(List.of("a", "b"), process.componentNames());
		assertEquals(QanaryProcessDefinition.sequential(List.of("a", "b")), process);
		assertEquals("[a, b]", process.toString());
	}

	@Test
	void aParallelBlockHoldsNamedStreamsInTheirOrder() {
		QanaryProcessDefinition process = read("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]");

		ParallelStep parallel = new ParallelStep(List.of( //
				new NamedStream("p1", List.of(component("b"))), //
				new NamedStream("x", List.of(component("c"), component("d")))));
		assertEquals(List.of(component("a"), parallel, component("e")), process.steps());
		assertFalse(process.isSequential());
		assertEquals(List.of("a", "b", "c", "d", "e"), process.componentNames());
		assertEquals("[a, {p1=[b], x=[c, d]}, e]", process.toString());
	}

	@Test
	void streamsMayContainParallelBlocksThemselves() {
		QanaryProcessDefinition process = read(
				"[{\"outer\": [\"a\", {\"inner1\": [\"b\"], \"inner2\": [\"c\"]}, \"d\"], \"other\": [\"e\"]}]");

		ParallelStep outer = (ParallelStep) process.steps().get(0);
		NamedStream stream = outer.streams().get(0);
		assertEquals("outer", stream.name());
		assertEquals(List.of("inner1", "inner2"),
				((ParallelStep) stream.steps().get(1)).streams().stream().map(NamedStream::name).toList());
		assertEquals(List.of("a", "b", "c", "d", "e"), process.componentNames());
	}

	@Test
	void aParallelBlockWithASingleStreamIsAllowed() {
		QanaryProcessDefinition process = read("[{\"only\": [\"a\", \"b\"]}]");

		assertEquals(List.of("a", "b"), process.componentNames());
		assertFalse(process.isSequential());
	}

	@Test
	void withoutComponentlistNothingIsExecuted() {
		RequestQuestionAnsweringProcess request = mapper.readValue("{\"question\": \"Who?\"}",
				RequestQuestionAnsweringProcess.class);

		assertSame(QanaryProcessDefinition.EMPTY, request.getProcessDefinition());
		assertTrue(read("null").isEmpty());
		assertTrue(read("[]").isEmpty());
		assertTrue(read("[]").isSequential());
	}

	@Test
	void equalDefinitionsAreEqualValues() {
		String componentlist = "[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]";
		QanaryProcessDefinition process = read(componentlist);
		QanaryProcessDefinition same = read(componentlist);

		assertEquals(process, same);
		assertEquals(process.hashCode(), same.hashCode());
		assertEquals(process.steps().get(1).hashCode(), same.steps().get(1).hashCode());
		// the order of the streams is kept (it is the order of the protocol and of the error reports)
		assertNotEquals(process, read("[\"a\", {\"x\": [\"c\", \"d\"], \"p1\": [\"b\"]}, \"e\"]"));
		assertNotEquals(process, read("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}]"));
		assertEquals("ParallelStep[streams=[NamedStream[name=p1, steps=[ComponentStep[componentName=b]]], "
				+ "NamedStream[name=x, steps=[ComponentStep[componentName=c], ComponentStep[componentName=d]]]]]",
				process.steps().get(1).toString());
	}

	@Test
	void theJsonStructureIsWrittenAsItWasRead() {
		String componentlist = "[\"a\",{\"p1\":[\"b\"],\"x\":[\"c\",\"d\"]},\"e\"]";

		assertEquals(componentlist, mapper.writeValueAsString(read(componentlist)));
		assertEquals(read(componentlist), QanaryProcessDefinition.fromJson(read(componentlist).toJson()));
	}

	@Test
	void componentNamesAreTakenAsTheyAre() {
		// unknown names are reported as unavailable components when the process is executed (as before)
		assertEquals(List.of("", "  ", "a b"), read("[\"\", \"  \", \"a b\"]").componentNames());
	}

	// ---- the Java API -------------------------------------------------------------------

	@Test
	void aSequentialProcessDefinitionIsCreatedFromComponentNames() {
		assertSame(QanaryProcessDefinition.EMPTY, QanaryProcessDefinition.sequential(null));
		assertTrue(QanaryProcessDefinition.sequential(List.of()).isEmpty());
		assertSame(QanaryProcessDefinition.EMPTY, QanaryProcessDefinition.fromJson(null));
	}

	@Test
	void theRequestKeepsTheSequentialAccessors() {
		RequestQuestionAnsweringProcess request = new RequestQuestionAnsweringProcess();
		request.setcomponentlist(List.of("a", "b"));
		assertEquals(QanaryProcessDefinition.sequential(List.of("a", "b")), request.getProcessDefinition());

		request.setProcessDefinition(read("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\"]}]"));
		@SuppressWarnings("deprecation")
		List<String> names = request.getcomponentlist();
		assertEquals(List.of("a", "b", "c"), names);
		assertTrue(request.toString().contains(" -- componentList: [a, {p1=[b], x=[c]}]"), request.toString());

		request.setProcessDefinition(null);
		assertSame(QanaryProcessDefinition.EMPTY, request.getProcessDefinition());
	}

	@Test
	void fromJsonAcceptsTheStructureOfListsMapsAndStrings() {
		Map<String, Object> block = new LinkedHashMap<>();
		block.put("p1", List.of("b"));
		block.put("x", List.of("c", "d"));

		assertEquals(read("[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]"),
				QanaryProcessDefinition.fromJson(List.of("a", block, "e")));
	}

	@Test
	void theStepsCannotBeChangedAfterwards() {
		QanaryProcessDefinition process = read("[\"a\", {\"p1\": [\"b\"]}]");

		assertThrows(UnsupportedOperationException.class, () -> process.steps().add(component("c")));
		ParallelStep parallel = (ParallelStep) process.steps().get(1);
		assertThrows(UnsupportedOperationException.class, () -> parallel.streams().clear());
		assertThrows(UnsupportedOperationException.class, () -> parallel.streams().get(0).steps().clear());
	}

	@Test
	void invalidStepsCannotBeCreated() {
		assertThrows(NullPointerException.class, () -> new ComponentStep(null));
		assertThrows(InvalidProcessDefinitionException.class, () -> new ParallelStep(List.of()));
		assertThrows(InvalidProcessDefinitionException.class, () -> new NamedStream(null, List.of(component("a"))));
		assertThrows(InvalidProcessDefinitionException.class, () -> new NamedStream("p1", List.of()));
		NamedStream stream = new NamedStream("p1", List.of(component("a")));
		InvalidProcessDefinitionException e = assertThrows(InvalidProcessDefinitionException.class,
				() -> new QanaryProcessDefinition(List.of(new ParallelStep(List.of(stream)),
						new ParallelStep(List.of(stream)))));
		assertTrue(e.getMessage().contains("\"p1\" is used more than once"), e.getMessage());
		assertThrows(NullPointerException.class,
				() -> QanaryProcessDefinition.sequential(Arrays.asList("a", null)));
	}

	// ---- invalid process definitions ------------------------------------------------------

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', value = { //
			"\"a\"                                   | the componentlist has to be an array of component names and parallel blocks, but it is the string \"a\"",
			"{\"p1\": [\"a\"]}                       | the componentlist has to be an array of component names and parallel blocks, but it is an object",
			"5                                       | the componentlist has to be an array of component names and parallel blocks, but it is the value 5",
			"[1]                                     | the componentlist contains the value 1; allowed are component names (strings) and parallel blocks (objects)",
			"[1.5]                                   | the componentlist contains the value 1.5",
			"[true]                                  | the componentlist contains the value true",
			"[null]                                  | the componentlist contains null",
			"[[\"a\"]]                               | the componentlist contains an array",
			"[{}]                                    | a parallel block ({}) needs at least one stream",
			"[{\"p1\": []}]                          | the parallel stream \"p1\" contains no component",
			"[{\"p1\": \"b\"}]                       | the parallel stream \"p1\" has to be an array of component names and parallel blocks, but it is the string \"b\"",
			"[{\"p1\": null}]                        | the parallel stream \"p1\" has to be an array of component names and parallel blocks, but it is null",
			"[{\"p1\": [5]}]                         | the parallel stream \"p1\" contains the value 5",
			"[{\"p1\": [\"a\", {\"x\": [false]}]}]   | the parallel stream \"x\" contains the value false",
			"[{\"\": [\"a\"]}]                       | the name of a parallel stream must not be empty",
			"[{\" \": [\"a\"]}]                      | the name of a parallel stream must not be empty",
			"[{\"p1\": [\"a\"], \"p1\": [\"b\"]}]    | the stream name \"p1\" is used more than once",
			"[{\"p1\": [\"a\"]}, \"b\", {\"p1\": [\"c\"]}]       | the stream name \"p1\" is used more than once",
			"[{\"p1\": [\"a\", {\"p1\": [\"c\"]}]}]  | the stream name \"p1\" is used more than once",
			"[{\"p1\": [\"a\", {\"x\": [\"c\"], \"x\": [\"d\"]}]}]  | the stream name \"x\" is used more than once" })
	void invalidProcessDefinitionsAreRejectedWithAnExplanation(String componentlist, String expectedMessage) {
		JacksonException e = assertThrows(JacksonException.class, () -> read(componentlist));

		InvalidProcessDefinitionException invalid = invalidProcessDefinitionIn(e);
		assertNotNull(invalid, "the cause of " + e);
		assertTrue(invalid.getMessage().startsWith(expectedMessage),
				"expected: " + expectedMessage + "\nactual:   " + invalid.getMessage());
	}

	private static InvalidProcessDefinitionException invalidProcessDefinitionIn(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof InvalidProcessDefinitionException invalid) {
				return invalid;
			}
		}
		return null;
	}
}
