package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by didier on 25.04.16.
 */
public class URIDetector {
    private static final Logger logger = LoggerFactory.getLogger(URIDetector.class);

    private final Set<Node> subjects = new HashSet<Node>();
    private final Set<Node> predicates = new HashSet<Node>();
    private final Set<Node> objects = new HashSet<Node>();

    public URIDetector(String sparqlQuery) {
        processQuery(sparqlQuery);
    }

    public Set<Node> getSubjects() {
        return subjects;
    }

    public Set<Node> getPredicates() {
        return predicates;
    }

    public Set<Node> getObjects() {
        return objects;
    }

    private void processQuery(String query) {
        Query q = QueryFactory.create(query); // SPARQL 1.1

        // This will walk through all parts of the query
        ElementWalker.walk(q.getQueryPattern(),
                // For each element...
                new ElementVisitorBase() {
                    // ...when it's a block of triples...
                    public void visit(ElementPathBlock el) {
                        // ...go through all the triples...
                        Iterator<TriplePath> triples = el.patternElts();
                        while (triples.hasNext()) {
                            // ...and grab the subject
                            TriplePath triple = triples.next();
                            subjects.add(triple.getSubject());
                            predicates.add(triple.getPredicate());
                            objects.add(triple.getObject());
                        }
                    }
                });
        logger.debug("subjects: {}", subjects);
        logger.debug("predicates: {}", predicates);
        logger.debug("objects: {}", objects);
    }
}
