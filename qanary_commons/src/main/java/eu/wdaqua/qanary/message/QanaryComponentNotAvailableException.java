package eu.wdaqua.qanary.message;

/**
 * represent the problem that a component was requested but not available
 *
 * @author AnBo
 */
public class QanaryComponentNotAvailableException extends Exception {
	private static final long serialVersionUID = -3926884955403862158L;

	// Constructor that accepts a message
    public QanaryComponentNotAvailableException(String message) {
        super(message);
    }
}
