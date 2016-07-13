package eu.wdaqua.qanary.message;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import eu.wdaqua.qanary.business.QanaryComponent;

/**
 * message that is send to the caller at the end of the QuestionAnswering process
 *
 * @author AnBo
 */
public class QanaryQuestionAnsweringFinished {
    // holds the ID of the QA process
    private UUID questionanswering;

    // start at point in time in milliseconds
    private long start;

    // start at point in time in milliseconds
    private long end;

    // trivial protocol
    private List<String> protocol;

    public void startQuestionAnswering() {
        this.start = System.currentTimeMillis();
        this.protocol = new LinkedList<>();
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

    public String toString() {
        return " question answering " + questionanswering + " took " + (this.end - this.start) + " ms, " + protocol;
    }

    /**
     * save the status of the run
     */
    public void appendProtocol(QanaryComponent component) {
        this.protocol.add(component.getName() + " " + component.getUrl());
    }
}
