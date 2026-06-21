package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.apache.jena.graph.Node;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.URIDetector;

class URIDetectorTest {

    @Test
    void detectsSubjectsPredicatesAndObjectsOfAQueryPattern() {
        String sparql = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?x WHERE { dbr:Berlin dbo:country ?x . }";

        URIDetector detector = new URIDetector(sparql);

        Set<Node> subjects = detector.getSubjects();
        Set<Node> predicates = detector.getPredicates();
        Set<Node> objects = detector.getObjects();

        assertEquals(1, subjects.size());
        assertTrue(subjects.iterator().next().isURI());
        assertEquals("http://dbpedia.org/resource/Berlin", subjects.iterator().next().getURI());

        assertEquals(1, predicates.size());
        assertEquals("http://dbpedia.org/ontology/country", predicates.iterator().next().getURI());

        assertEquals(1, objects.size());
        // ?x is a variable, not a URI
        assertTrue(objects.iterator().next().isVariable());
    }

    @Test
    void handlesMultipleTriplePatterns() {
        String sparql = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?x ?y WHERE { dbr:Berlin dbo:country ?x . ?x dbo:capital ?y . }";

        URIDetector detector = new URIDetector(sparql);

        // dbr:Berlin and ?x appear as subjects
        assertEquals(2, detector.getSubjects().size());
        // dbo:country and dbo:capital
        assertEquals(2, detector.getPredicates().size());
    }
}
