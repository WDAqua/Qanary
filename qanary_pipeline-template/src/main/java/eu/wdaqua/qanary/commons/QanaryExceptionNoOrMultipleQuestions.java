package eu.wdaqua.qanary.commons;

/**
 * Created by Dennis on 30/03/17.
 */
public class QanaryExceptionNoOrMultipleQuestions extends Exception {
    // Constructor that accepts a message
    public QanaryExceptionNoOrMultipleQuestions(String message) {
        super(message);
    }
}
