package eu.wdaqua.qanary.web.messages;

import java.net.URI;
import java.time.Instant;

/**
 * One error observed by the Qanary pipeline while answering a specific question.
 * <p>
 * Errors are collected per question URI so that a developer can ask the pipeline
 * what went wrong for a given question (see
 * {@code GET /questionanswering/errors?questionuri=...}): which component failed,
 * whether the triplestore was not accessible, or whether the pipeline failed.
 *
 * @author Qanary
 */
public class QanaryQuestionAnsweringError {

    public enum ErrorType {
        /** a requested component is not registered with / not reachable from the pipeline */
        COMPONENT_NOT_AVAILABLE,
        /** a component was called but did not return a successful response */
        COMPONENT_EXECUTION_FAILED,
        /** the Qanary triplestore could not be reached or a SPARQL query failed */
        TRIPLESTORE_NOT_ACCESSIBLE,
        /** any other failure during the question-answering process */
        PIPELINE_FAILURE
    }

    private final URI questionUri;
    private final ErrorType type;
    private final String component; // null when the error is not component-specific
    private final String message;
    private final String timestamp; // ISO-8601 instant

    public QanaryQuestionAnsweringError(URI questionUri, ErrorType type, String component, String message) {
        this.questionUri = questionUri;
        this.type = type;
        this.component = component;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public URI getQuestionUri() {
        return questionUri;
    }

    public ErrorType getType() {
        return type;
    }

    public String getComponent() {
        return component;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
