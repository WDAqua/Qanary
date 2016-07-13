package eu.wdaqua.qanary.message;

import java.net.URI;

import org.springframework.boot.orm.jpa.EntityScan;

import eu.wdaqua.qanary.web.QanarySparqlController;

/**
 * message object send back to client by /question endpoint in {@link QanarySparqlController}
 *
 * @author AnBo
 */
@EntityScan
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
