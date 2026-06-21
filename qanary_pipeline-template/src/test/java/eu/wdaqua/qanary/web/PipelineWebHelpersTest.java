package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.web.messages.AdditionalInsertQuery;

class PipelineWebHelpersTest {

    @Test
    void stringToInsertQueryConverterWrapsValidQuery() {
        StringToInsertQueryConverter converter = new StringToInsertQueryConverter();
        AdditionalInsertQuery result = converter.convert("INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> <urn:o> } }");
        assertNotNull(result);
        assertNotNull(result.getInsertQuery());
    }

    @Test
    void stringToInsertQueryConverterPassesThroughInvalidQuery() {
        StringToInsertQueryConverter converter = new StringToInsertQueryConverter();
        AdditionalInsertQuery result = converter.convert("SELECT * WHERE { ?s ?p ?o }");
        assertNotNull(result);
        assertNull(result.getInsertQuery());
    }

    @Test
    void springBootAdminCompatibilityResultExposesId() {
        // the Result message is a non-static inner class of the redirect controller
        QanarySpringBootAdminCompatibilityRedirectController controller =
                new QanarySpringBootAdminCompatibilityRedirectController();
        QanarySpringBootAdminCompatibilityRedirectController.Result result = controller.new Result("instance-123");
        assertEquals("instance-123", result.getId());
    }
}
