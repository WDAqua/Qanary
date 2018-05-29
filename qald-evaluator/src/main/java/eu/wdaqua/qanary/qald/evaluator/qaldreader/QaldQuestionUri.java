package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QaldQuestionUri {
    private static final Logger logger = LoggerFactory.getLogger(QaldQuestionUri.class);

    private URI uri = null;
    private boolean isUsedAsSubject = false;
    private boolean isUsedAsPredicate = false;
    private boolean isUsedAsObject = false;
    private LinkedList<Integer> usedInQuestions = new LinkedList<>();

    public QaldQuestionUri(int qaldId, String uri) {

        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            logger.error("No URI from QALD question: {}, should had never ever happened.", uri);
            e.printStackTrace();
            this.uri = null;
        }

        this.alsoUsedInQaldQuestion(qaldId);

    }

    /**
     * true if URI starts with http://dbpedia.org/resource/
     */
    public boolean isDBpediaResource() {
        return this.getUri().toString().startsWith("http://dbpedia.org/resource/");
    }

    /**
     * true if URI starts with http://dbpedia.org/ontology/
     */
    public boolean isDBpediaConcept() {
        return this.getUri().toString().startsWith("http://dbpedia.org/ontology/");
    }

    /**
     * true if URI starts with http://dbpedia.org/property/
     */
    public boolean isDBpediaProperty() {
        return this.getUri().toString().startsWith("http://dbpedia.org/property/");
    }

    /**
     * true if URI starts with http://www.w3.org/1999/02/22-rdf-syntax-ns#
     */
    public boolean isRdfSyntax() {
        return this.getUri().toString().startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }

    public URI getUri() {
        return this.uri;
    }

    public void setIsUsedAsSubject() {
        this.isUsedAsSubject = true;
    }

    public boolean isUsedAsSubject() {
        return this.isUsedAsSubject;
    }

    public void setIsUsedAsPredicate() {
        this.isUsedAsPredicate = true;
    }

    public boolean isUsedAsPredicate() {
        return this.isUsedAsPredicate;
    }

    public void setIsUsedAsObject() {
        this.isUsedAsObject = true;
    }

    public boolean isUsedAsObject() {
        return this.isUsedAsObject;
    }

    /**
     * register a QALD question number where this uri was used
     */
    public void alsoUsedInQaldQuestion(int qaldId) {
        this.usedInQuestions.add(qaldId);
    }

}
