package eu.wdaqua.qanary.web.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * Edge cases of the request messages of the pipeline not covered by
 * {@link PipelineMessagesTest}.
 */
class PipelineMessagesCoverageTest {

    @Test
    void insertQueryWithSyntaxErrorsIsDropped() {
        assertNull(new AdditionalInsertQuery("INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> } }").getInsertQuery());
    }

    @Test
    void additionalTriplesAreWrittenToATurtleFile(@TempDir Path directory) throws Exception {
        Path tripleDirectory = directory.resolve("additional-triples");
        String turtle = "<urn:s> <urn:p> \"o\" .";

        AdditionalTriples triples = new AdditionalTriples(turtle, new MockEnvironment()
                .withProperty("qanary.process.additional-triples-directory", tripleDirectory.toString()));

        assertEquals(turtle, triples.getTriples());
        assertEquals(turtle, Files.readString(Path.of(triples.getStringFilePath())));
        assertTrue(triples.getStringFilePath().endsWith(triples.getUUIDString() + ".ttl"));
        assertNotNull(triples.getUriFilePath());
    }

    @Test
    void blankAdditionalTriplesWriteNoFile(@TempDir Path directory) throws Exception {
        AdditionalTriples triples = new AdditionalTriples("   ", new MockEnvironment()
                .withProperty("qanary.process.additional-triples-directory", directory.toString()));

        assertNull(triples.getStringFilePath());
        assertNull(triples.getUriFilePath());
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void additionalTriplesThatCannotBeStoredAreSkipped(@TempDir Path directory) throws Exception {
        Path occupied = Files.writeString(directory.resolve("not-a-directory"), "x");

        AdditionalTriples triples = new AdditionalTriples("<urn:s> <urn:p> <urn:o> .", new MockEnvironment()
                .withProperty("qanary.process.additional-triples-directory", occupied.toString()));

        assertEquals("<urn:s> <urn:p> <urn:o> .", triples.getTriples());
        assertTrue(Files.isRegularFile(occupied));
    }

    @Test
    void requestCarriesItsTargetData() {
        RequestQuestionAnsweringProcess request = new RequestQuestionAnsweringProcess();
        request.setTargetdata(List.of("dbpedia", "wikidata"));

        assertEquals(List.of("dbpedia", "wikidata"), request.getTargetdata());
    }
}
