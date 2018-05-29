package eu.wdaqua.qanary.message;

/**
 * represent the problem that a component was requested but not available
 *
 * @author AnBo
 */
public class QanaryComponentNotAvailableException extends Exception {
    // Constructor that accepts a message
    public QanaryComponentNotAvailableException(String message) {
        super(message);
    }
}
