package eu.wdaqua.qanary.message;

public class QanaryExceptionQuestionNotProvided extends Exception {

	private static final long serialVersionUID = 7935029187153605509L;

	public QanaryExceptionQuestionNotProvided() {
		super("A question (string) is required. Process not started.");
	}

}
