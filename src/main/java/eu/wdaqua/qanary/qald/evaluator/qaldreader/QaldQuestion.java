package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * represents the information extracted from the QALD-6 question
 *
 * @author AnBo
 */
public class QaldQuestion {
    private static final Logger logger = LoggerFactory.getLogger(QaldQuestion.class);

    /**
     * contains all the annotated URIs of the provided question
     */
    private HashMap<String, QaldQuestionUri> uris = new HashMap<>();
    private final int qaldId;
    private String question;

    /**
     * might be null if not provided by QALD
     */
    private String sparqlQuery;

    /**
     * process the QALD-6 question object, extract the relevant information
     */
    public enum NodePosition {
        SUBJECT, PREDICATE, OBJECT
    }

    public QaldQuestion(JsonObject qaldQuestion) {

        JsonArray questiondata;
        String language;
        String questionstring;
        JsonObject query;
        String sparqlquery;
        URIDetector uriDetector;

        questiondata = qaldQuestion.getAsJsonArray("question");
        qaldId = qaldQuestion.get("id").getAsInt();

        // check all languages until "en" was found
        for (int j = 0; j < questiondata.size(); j++) {
            language = questiondata.get(j).getAsJsonObject().get("language").getAsString();
            if (language.compareTo("en") == 0) {
                questionstring = questiondata.get(j).getAsJsonObject().get("string").getAsString();
                this.setQuestion(questionstring);

                query = qaldQuestion.get("query").getAsJsonObject();

                // check if SPARQL query is available
                if (query.isJsonObject() && !query.isJsonNull() && query.has("sparql")) {
                    this.setSparqlQuery(qaldQuestion.get("query").getAsJsonObject().get("sparql").getAsString());

                    logger.debug("QALD no. {}: '{}' leads to SPARQL query: '{}'", qaldId, questionstring,
                            this.getSparqlQuery());

                    uriDetector = new URIDetector(this.getSparqlQuery());

                    addNodeToLists(qaldId, uriDetector.getSubjects(), NodePosition.SUBJECT);
                    addNodeToLists(qaldId, uriDetector.getPredicates(), NodePosition.PREDICATE);
                    addNodeToLists(qaldId, uriDetector.getObjects(), NodePosition.OBJECT);
                    break;
                }
            } else {
                sparqlquery = null;
                // actually this can happending considering the QALD-6 data
                logger.warn("No SPARQL query found in {}.", qaldQuestion);
            }

        }

    }

    /**
     * returns the instance of QaldQuestionUri which corresponds to the provided uri, if none exists
     * create a new one
     */
    private QaldQuestionUri createQaldQuestionUri(int qaldId, String uri) {
        if (!this.uris.containsKey(uri)) {
            QaldQuestionUri qaldQuestionUri = new QaldQuestionUri(qaldId, uri);
            this.uris.put(uri, qaldQuestionUri);
        } else {
            this.uris.get(uri).alsoUsedInQaldQuestion(qaldId);
        }
        return this.uris.get(uri);
    }

    /**
     * save all nodes which are URIs as QaldQuestionUri objects
     */
    private void addNodeToLists(int qaldId, Set<Node> nodes, NodePosition nodePosition) {
        QaldQuestionUri qaldQuestionUri;
        for (Node node : nodes) {
            if (node.isURI()) {
                qaldQuestionUri = this.createQaldQuestionUri(qaldId, node.getURI());

                if (nodePosition == NodePosition.SUBJECT) {
                    qaldQuestionUri.setIsUsedAsSubject();
                }
                if (nodePosition == NodePosition.PREDICATE) {
                    qaldQuestionUri.setIsUsedAsPredicate();
                }
                if (nodePosition == NodePosition.OBJECT) {
                    qaldQuestionUri.setIsUsedAsObject();
                }
            }
        }
    }

    /**
     * returns the number of the QALD question in the parsed JSON file
     */
    public int getQaldId() {
        return this.qaldId;
    }

    /**
     * retrieve all annotated URIs from the current question
     */
    public Collection<QaldQuestionUri> getUris() {
        return this.uris.values();
    }

    /**
     * retrieve the instance of QaldQuestionUri which is identified by the provided uri
     */
    public QaldQuestionUri getUri(URI uri) {
        return this.uris.get(uri.toString());
    }

    private void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    private String getSparqlQuery() {
        return this.sparqlQuery;
    }

    /**
     * set question string
     *
     * TODO: cover different languages
     */
    private void setQuestion(String question) {
        this.question = question;
    }

    /**
     * receive question string
     */
    public String getQuestion() {
        return this.question;
    }

    /**
     * returns all instances that are DBpedia resource URIs
     */
    private List<QaldQuestionUri> getResourceUris() {
        List<QaldQuestionUri> resourceUris = new LinkedList<>();
        QaldQuestionUri qaldQuestionUri;

        Iterator<QaldQuestionUri> iter = this.uris.values().iterator();
        while (iter.hasNext()) {
            qaldQuestionUri = iter.next();
            if (qaldQuestionUri.isDBpediaResource()) {
                resourceUris.add(qaldQuestionUri);
            }
        }

        return resourceUris;
    }

    /**
     * returns a list of strings representing the dbpedia:resource URIs
     */
    public List<String> getResourceUrisAsString() {
        List<String> result = new LinkedList<>();
        for (QaldQuestionUri uri : this.getResourceUris()) {
            result.add(uri.getUri().toString());
        }
        return result;
    }

}
