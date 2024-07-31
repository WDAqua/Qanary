package eu.wdaqua.qanary.exceptions;

import org.springframework.http.HttpStatus;

/**
 * represents the message of components that was callable but does not return a
 * valid response code
 *
 * @author AnBo
 */
public class QanaryExceptionServiceCallNotOk extends Exception {

	private static final long serialVersionUID = 7935029187153605509L;
	private long duration;
	private String errormessage;
	private String componentName;

	public QanaryExceptionServiceCallNotOk(String componentName, long duration, HttpStatus myHttpStatus) {
		super(String.format(
				"Call to Qanary component {} was not performed correctly (duration: {} ms); it returned {}. |\n{}", //
				componentName, //
				duration, //
				myHttpStatus.name(), //
				myHttpStatus.value() //
		));
		this.componentName = componentName;
		this.duration = duration;
		this.errormessage = myHttpStatus.name() + "/" + myHttpStatus.value();
	}

	public QanaryExceptionServiceCallNotOk(String componentName, long duration, String message, String stackTrace) {
		super(String.format(
				"Call to Qanary component {} was not performed correctly (duration: {} ms); it returned {}. |\n{}", //
				componentName, //
				duration, //
				message, //
				stackTrace));
		this.componentName = componentName;
		this.duration = duration;
		this.errormessage = message + "\n" + stackTrace;
	}

	public long getDuration() {
		return duration;
	}

	public String getErrormessageHttpStatus() {
		return errormessage;
	}

	public String getComponentName() {
		return componentName;
	}
}
