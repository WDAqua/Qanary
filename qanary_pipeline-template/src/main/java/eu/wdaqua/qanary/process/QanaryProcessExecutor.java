package eu.wdaqua.qanary.process;

import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.exceptions.ParallelStreamsFailedException;
import eu.wdaqua.qanary.exceptions.ParallelStreamsFailedException.StreamFailure;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.ComponentStep;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.NamedStream;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.ParallelStep;
import eu.wdaqua.qanary.process.QanaryProcessDefinition.Step;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * executes a {@link QanaryProcessDefinition}: the components of a sequence one
 * after another, the streams of a parallel block at the same time (each one in
 * its own thread)
 * <p>
 * Consecutive components are called by one
 * {@link QanaryConfigurator#callServices(List, QanaryMessage)} call, so a
 * process definition without parallel blocks is executed exactly as a list of
 * components always was. Every stream starts with the message the parallel
 * block was started with, and the block passes this message on to the next
 * step.
 * <p>
 * If a stream fails, it stops at the failed component; the other streams of the
 * block are completed (so which components are executed does not depend on
 * timing). Afterwards, a {@link ParallelStreamsFailedException} naming every
 * failed stream ends the process.
 */
public class QanaryProcessExecutor {

	/**
	 * is told about each component of the process definition that is not
	 * available
	 */
	@FunctionalInterface
	public interface UnavailableComponentListener {
		/**
		 * @param componentName the requested component, which will be skipped
		 * @param stream        the parallel stream the component belongs to, null if
		 *                      it does not belong to a parallel stream
		 */
		void componentNotAvailable(String componentName, String stream);
	}

	private static final Logger logger = LoggerFactory.getLogger(QanaryProcessExecutor.class);

	private final QanaryConfigurator configurator;
	private final Function<List<String>, List<QanaryComponent>> componentResolver;
	private final UnavailableComponentListener unavailableComponentListener;

	/**
	 * @param configurator                 calls the components
	 * @param componentResolver            returns the available components among
	 *                                     the given component names
	 * @param unavailableComponentListener is told about each requested component
	 *                                     that is not available
	 */
	public QanaryProcessExecutor(QanaryConfigurator configurator,
			Function<List<String>, List<QanaryComponent>> componentResolver,
			UnavailableComponentListener unavailableComponentListener) {
		this.configurator = configurator;
		this.componentResolver = componentResolver;
		this.unavailableComponentListener = unavailableComponentListener;
	}

	/**
	 * execute the process definition; components that are not available are
	 * reported and skipped
	 *
	 * @param process the components to be executed
	 * @param message the message every component receives
	 * @return the protocol of all executed components
	 * @throws QanaryExceptionServiceCallNotOk if a component of the main sequence
	 *                                         failed, or a
	 *                                         {@link ParallelStreamsFailedException}
	 *                                         if parallel streams failed
	 */
	public QanaryQuestionAnsweringFinished execute(QanaryProcessDefinition process, QanaryMessage message)
			throws QanaryExceptionServiceCallNotOk {
		Map<String, QanaryComponent> availableComponents = new HashMap<>();
		for (QanaryComponent component : componentResolver
				.apply(new ArrayList<>(new LinkedHashSet<>(process.componentNames())))) {
			availableComponents.put(component.getName(), component);
		}
		reportUnavailableComponents(process.steps(), null, availableComponents);

		QanaryQuestionAnsweringFinished protocol = new QanaryQuestionAnsweringFinished(message);
		protocol.startQuestionAnswering();
		run(process.steps(), message, availableComponents, protocol);
		protocol.endQuestionAnswering();
		return protocol;
	}

	private void reportUnavailableComponents(List<Step> steps, String stream,
			Map<String, QanaryComponent> availableComponents) {
		for (Step step : steps) {
			if (step instanceof ComponentStep component) {
				if (!availableComponents.containsKey(component.componentName())) {
					unavailableComponentListener.componentNotAvailable(component.componentName(), stream);
				}
			} else { // the only other kind of step
				ParallelStep parallel = (ParallelStep) step;
				for (NamedStream namedStream : parallel.streams()) {
					reportUnavailableComponents(namedStream.steps(), namedStream.name(), availableComponents);
				}
			}
		}
	}

	/**
	 * execute the steps one after another; consecutive components are called
	 * together
	 */
	private void run(List<Step> steps, QanaryMessage message, Map<String, QanaryComponent> availableComponents,
			QanaryQuestionAnsweringFinished protocol) throws QanaryExceptionServiceCallNotOk {
		List<QanaryComponent> consecutiveComponents = new ArrayList<>();
		for (Step step : steps) {
			if (step instanceof ComponentStep component) {
				QanaryComponent availableComponent = availableComponents.get(component.componentName());
				if (availableComponent != null) {
					consecutiveComponents.add(availableComponent);
				}
			} else { // the only other kind of step
				ParallelStep parallel = (ParallelStep) step;
				call(consecutiveComponents, message, protocol);
				consecutiveComponents = new ArrayList<>();
				runParallel(parallel, message, availableComponents, protocol);
			}
		}
		call(consecutiveComponents, message, protocol);
	}

	private void call(List<QanaryComponent> components, QanaryMessage message,
			QanaryQuestionAnsweringFinished protocol) throws QanaryExceptionServiceCallNotOk {
		if (!components.isEmpty()) {
			protocol.appendProtocol(configurator.callServices(components, message));
		}
	}

	/**
	 * run all streams of the parallel block at the same time and wait until all
	 * of them have finished
	 */
	private void runParallel(ParallelStep parallel, QanaryMessage message,
			Map<String, QanaryComponent> availableComponents, QanaryQuestionAnsweringFinished protocol)
			throws ParallelStreamsFailedException {
		long start = System.currentTimeMillis();
		logger.info("start {} parallel stream(s): {}", parallel.streams().size(),
				parallel.streams().stream().map(NamedStream::name).toList());

		List<Future<QanaryQuestionAnsweringFinished>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (NamedStream stream : parallel.streams()) {
				results.add(executor.submit(() -> runStream(stream, message, availableComponents)));
			}
		} // waits until all streams have finished

		List<StreamFailure> failures = new ArrayList<>();
		for (int i = 0; i < results.size(); i++) {
			Future<QanaryQuestionAnsweringFinished> result = results.get(i);
			String stream = parallel.streams().get(i).name();
			if (result.state() == Future.State.SUCCESS) {
				protocol.appendProtocol(result.resultNow());
			} else { // failed: the streams are never cancelled
				failures.addAll(failuresOf(stream, result.exceptionNow()));
			}
		}
		long duration = System.currentTimeMillis() - start;
		if (!failures.isEmpty()) {
			ParallelStreamsFailedException exception = new ParallelStreamsFailedException(failures, duration);
			logger.error("parallel block failed after {} ms: {}", duration, exception.getMessage());
			throw exception;
		}
		logger.info("all {} parallel stream(s) finished after {} ms", parallel.streams().size(), duration);
	}

	private QanaryQuestionAnsweringFinished runStream(NamedStream stream, QanaryMessage message,
			Map<String, QanaryComponent> availableComponents) throws QanaryExceptionServiceCallNotOk {
		// the thread name shows the stream in the log messages
		Thread.currentThread().setName("qanary-stream-" + stream.name());
		logger.info("parallel stream \"{}\" starts: {}", stream.name(), stream.steps());
		QanaryQuestionAnsweringFinished protocol = new QanaryQuestionAnsweringFinished(message);
		protocol.startQuestionAnswering();
		run(stream.steps(), message, availableComponents, protocol);
		protocol.endQuestionAnswering();
		logger.info("parallel stream \"{}\" finished after {} ms", stream.name(), protocol.getDurationInMilliseconds());
		return protocol;
	}

	/**
	 * the failures a stream ended with; the failures of a nested parallel block
	 * keep the names of their own streams
	 */
	private static List<StreamFailure> failuresOf(String stream, Throwable failure) {
		if (failure instanceof ParallelStreamsFailedException nested) {
			return nested.getFailures();
		} else if (failure instanceof QanaryExceptionServiceCallNotOk failedCall) {
			return List.of(new StreamFailure(stream, failedCall.getComponentName(), failedCall));
		} else if (failure instanceof Error error) {
			throw error;
		} else {
			return List.of(new StreamFailure(stream, null, failure));
		}
	}
}
