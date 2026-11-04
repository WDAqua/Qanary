package eu.wdaqua.qanary.exceptions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * at least one parallel stream of a question answering process failed; the
 * process was stopped after all streams of the parallel block had finished
 * <p>
 * Every failure names the stream it happened in (the property name given in the
 * process definition) and, if known, the failed component.
 * {@link #getComponentName()} returns the component of the first failure (in the
 * order of the process definition).
 */
public class ParallelStreamsFailedException extends QanaryExceptionServiceCallNotOk {

	private static final long serialVersionUID = -3043377432905361637L;

	/**
	 * the failure of one parallel stream
	 *
	 * @param stream    the name of the stream (innermost one for nested parallel
	 *                  blocks)
	 * @param component the name of the component that failed, null if the failure
	 *                  is not related to a component
	 * @param cause     what went wrong
	 */
	public record StreamFailure(String stream, String component, Throwable cause) {

		@Override
		public String toString() {
			// only the first line: the message of a failed component call includes a stack trace
			String message = cause.getMessage() == null ? cause.getClass().getName()
					: cause.getMessage().lines().findFirst().orElse("");
			return "stream \"" + stream + "\"" //
					+ (component == null ? "" : ", component \"" + component + "\"") //
					+ ": " + message;
		}
	}

	private final transient List<StreamFailure> failures;

	/**
	 * @param failures the failures of the streams, at least one
	 * @param duration the time the parallel block was running (in milliseconds)
	 */
	public ParallelStreamsFailedException(List<StreamFailure> failures, long duration) {
		super(message(failures), failures.get(0).component(), duration);
		this.failures = List.copyOf(failures);
		for (StreamFailure failure : failures) {
			this.addSuppressed(failure.cause());
		}
	}

	private static String message(List<StreamFailure> failures) {
		return failures.size() + " parallel stream(s) failed: " //
				+ failures.stream().map(StreamFailure::toString).collect(Collectors.joining(" | "));
	}

	public List<StreamFailure> getFailures() {
		return failures;
	}

}
