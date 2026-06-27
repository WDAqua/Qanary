package eu.wdaqua.qanary.message;

import java.net.URI;

/**
 * message object send back to client by /question endpoint in @see eu.wdaqua.qanary.QanarySparqlController
 *
 * @author AnBo
 */
public class QanaryQuestionCreated {
    private final URI questionURI;
    private final String questionID;

    public QanaryQuestionCreated(String fileid, URI questionURI) {
        this.questionURI = questionURI;
        this.questionID = fileid;
    }

    public URI getQuestionURI() {
        return this.questionURI;
    }

    public String getQuestionID() {
        return this.questionID;
    }
}
