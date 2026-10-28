package eu.wdaqua.qanary.exceptions;

/**
 * the component list (process definition) of a request to start a question
 * answering process is not valid; nothing was executed
 */
public class InvalidProcessDefinitionException extends IllegalArgumentException {

	private static final long serialVersionUID = 4113528712301917305L;

	public InvalidProcessDefinitionException(String message) {
		super(message);
	}

}
