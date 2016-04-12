package eu.wdaqua.qanary.message;

/**
 * message that is send to the caller at the end of the QuestionAnswering
 * process
 * 
 * @author AnBo
 *
 */
public class QanaryQuestionAnsweringFinished {
	// start at point in time in milliseconds
	long start;

	// start at point in time in milliseconds
	long end;

	public void startQuestionAnswering() {
		this.start = System.currentTimeMillis();
	}

	public long getStartOfQuestionAnswering() {
		return this.start;
	}

	public void endQuestionAnswering() {
		this.end = System.currentTimeMillis();
	}

	public long getendOfQuestionAnswering() {
		return this.end;
	}
}
