package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestionUri;

class QaldQuestionUriTest {

    @Test
    void classifiesDBpediaResource() {
        QaldQuestionUri uri = new QaldQuestionUri(1, "http://dbpedia.org/resource/Berlin");
        assertTrue(uri.isDBpediaResource());
        assertFalse(uri.isDBpediaConcept());
        assertFalse(uri.isDBpediaProperty());
        assertFalse(uri.isRdfSyntax());
        assertEquals("http://dbpedia.org/resource/Berlin", uri.getUri().toString());
    }

    @Test
    void classifiesDBpediaConceptPropertyAndRdfSyntax() {
        assertTrue(new QaldQuestionUri(1, "http://dbpedia.org/ontology/Country").isDBpediaConcept());
        assertTrue(new QaldQuestionUri(1, "http://dbpedia.org/property/population").isDBpediaProperty());
        assertTrue(new QaldQuestionUri(1, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type").isRdfSyntax());
    }

    @Test
    void tracksPositionFlags() {
        QaldQuestionUri uri = new QaldQuestionUri(1, "http://dbpedia.org/resource/Berlin");
        assertFalse(uri.isUsedAsSubject());
        assertFalse(uri.isUsedAsPredicate());
        assertFalse(uri.isUsedAsObject());

        uri.setIsUsedAsSubject();
        uri.setIsUsedAsPredicate();
        uri.setIsUsedAsObject();

        assertTrue(uri.isUsedAsSubject());
        assertTrue(uri.isUsedAsPredicate());
        assertTrue(uri.isUsedAsObject());
    }

    @Test
    void registersAdditionalQaldQuestionsWithoutError() {
        QaldQuestionUri uri = new QaldQuestionUri(1, "http://dbpedia.org/resource/Berlin");
        uri.alsoUsedInQaldQuestion(2);
        uri.alsoUsedInQaldQuestion(3);
        assertTrue(uri.isDBpediaResource());
    }

    @Test
    void invalidUriIsHandledGracefully() {
        // an illegal URI string must not throw; the class logs and stores null
        QaldQuestionUri uri = new QaldQuestionUri(1, "http://example.org/ has spaces");
        org.junit.jupiter.api.Assertions.assertNull(uri.getUri());
    }
}
