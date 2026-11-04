package eu.wdaqua.qanary.web;

/**
 * values from requests are logged without line breaks: otherwise, a question
 * could forge log entries
 */
final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * @return the text of the value, with spaces instead of line breaks and tabs
     */
    static String sanitize(Object value) {
        return String.valueOf(value).replaceAll("[\n\r\t]", " ");
    }
}
