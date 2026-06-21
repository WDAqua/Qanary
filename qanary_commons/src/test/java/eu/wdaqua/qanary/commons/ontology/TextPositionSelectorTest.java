package eu.wdaqua.qanary.commons.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.apache.jena.reasoner.IllegalParameterException;
import org.junit.jupiter.api.Test;

class TextPositionSelectorTest {

    @Test
    void startEndConstructorStoresRangeAndLeavesUriAndScoreNull() {
        TextPositionSelector s = new TextPositionSelector(3, 10);
        assertEquals(3, s.getStart());
        assertEquals(10, s.getEnd());
        assertNull(s.getResourceUri());
        assertNull(s.getScore());
    }

    @Test
    void scoreConstructorStoresScore() {
        TextPositionSelector s = new TextPositionSelector(0, 5, 0.75f);
        assertEquals(0.75f, s.getScore());
        assertNull(s.getResourceUri());
    }

    @Test
    void uriConstructorStoresResourceUri() {
        URI uri = URI.create("http://dbpedia.org/resource/Berlin");
        TextPositionSelector s = new TextPositionSelector(0, 5, uri, 0.5f);
        assertEquals(uri, s.getResourceUri());
        assertEquals(0.5f, s.getScore());
    }

    @Test
    void stringUriConstructorParsesUri() throws Exception {
        TextPositionSelector s = new TextPositionSelector(0, 5, "http://dbpedia.org/resource/Berlin", 0.5f);
        assertEquals(URI.create("http://dbpedia.org/resource/Berlin"), s.getResourceUri());
    }

    @Test
    void zeroBoundaryIsAccepted() {
        // start == 0, end == 0 (and end == start) are the valid lower boundary;
        // pins the 'start < 0' / 'end < 0' / 'end - start < 0' checks against being
        // weakened to '<=' (kills the ConditionalsBoundary mutants flagged by PIT).
        TextPositionSelector s = new TextPositionSelector(0, 0);
        assertEquals(0, s.getStart());
        assertEquals(0, s.getEnd());

        TextPositionSelector range = new TextPositionSelector(0, 1);
        assertEquals(0, range.getStart());
        assertEquals(1, range.getEnd());
    }

    @Test
    void negativeStartIsRejected() {
        assertThrows(IllegalParameterException.class, () -> new TextPositionSelector(-1, 5));
    }

    @Test
    void negativeEndIsRejected() {
        assertThrows(IllegalParameterException.class, () -> new TextPositionSelector(0, -5));
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThrows(IllegalParameterException.class, () -> new TextPositionSelector(10, 3));
    }
}
