package eu.wdaqua.qanary.message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.core.util.datetime.Format;
import org.springframework.http.HttpStatus;

import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.commons.QanaryMessage;

/**
 * message that is send to the caller at the end of the QuestionAnswering
 * process
 *
 * @author AnBo
 */
public class QanaryQuestionAnsweringFinished {
	
	private class ComponentExecutionLog {
		private String componentName;
		private String componentUri;
		private long time;
		private int httpResponseCode;
		
		private ComponentExecutionLog(String componentName, String componentUri, long time, int httpResponseCode) {
			this.componentName = componentName;
			this.componentUri = componentUri;
			this.time = time;
			this.httpResponseCode = httpResponseCode;
		}
		
		public long getDurationInMilliseconds() {
			return this.time;
		}
		
		public int getHttpResponseCode() {
			return this.httpResponseCode;
		}
		
		public String getComponentName() {
			return this.componentName;
		}
		
		public String getComponentUri() {
			return this.componentUri;
		}
	}
	
	// holds the ID of the QA process
	private UUID questionanswering = UUID.randomUUID();

	// start at point in time in milliseconds
	private long start;

	// start at point in time in milliseconds
	private long end;

	// trivial protocol
	private List<String> protocol;

	// extended protocol
	private List<ComponentExecutionLog> extendedProtocol;
	
	private QanaryMessage message;

	public QanaryQuestionAnsweringFinished(QanaryMessage message) {
		this.message = message;
	}

	public void startQuestionAnswering() {
		this.start = System.currentTimeMillis();
		this.protocol = new LinkedList<>();
		this.extendedProtocol = new LinkedList<>();
	}

	private String getReadableDate(long time) {
		Date date = new Date(time);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(date);
	}
	public String getStartOfQuestionAnswering() {
		return this.getReadableDate(this.start);
	}

	public void endQuestionAnswering() {
		this.end = System.currentTimeMillis();
	}
	
	public long getDurationInMilliseconds() {
		return this.end - this.start;
	}

	public String getEndOfQuestionAnswering() {
		return this.getReadableDate(this.end);
	}
	
	public QanaryMessage getQanaryMessage() {
		return this.message;
	}

	public String toString() {
		return " question answering " + questionanswering + " took " + (this.end - this.start) + " ms, " + protocol;
	}

	/**
	 * save the status of the run
	 * @param duration 
	 * @param httpStatus 
	 */
	public void appendProtocol(QanaryComponent component, HttpStatus httpStatus, long duration) {
		int httpStatusCode = httpStatus.value();
		this.protocol.add(component.getName() + " " + component.getUrl()+ " " + httpStatusCode);
		ComponentExecutionLog componentExecutionLog = new ComponentExecutionLog(component.getName(), component.getUrl(), duration, httpStatusCode);
		this.extendedProtocol.add(componentExecutionLog);
	}

	public List<String> getCompactProtocol() {
		return this.protocol;
	}

	public List<ComponentExecutionLog> getExtendedProtocol() {
		return this.extendedProtocol;
	}
}
