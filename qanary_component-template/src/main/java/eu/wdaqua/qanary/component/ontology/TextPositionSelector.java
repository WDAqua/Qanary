package eu.wdaqua.qanary.component.ontology;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.reasoner.IllegalParameterException;

/**
 * represents the textpositionselector concept of the W3C OpenAnnotation vocabulary {@linktourl
 * https://www.w3.org/ns/oa#d4e667}
 *
 * TODO: implement a builder pattern for creating objects
 *
 * @author AnBo
 */
public class TextPositionSelector {
    private int start;
    private int end;
    private URI resourceUri;
    private Float score;

    public TextPositionSelector(int start, int end) throws IllegalParameterException {
        if (start < 0) {
            throw new IllegalParameterException(
                    "start was lower than 0, selections have to be positive (was " + start + ").");
        }
        if (end < 0) {
            throw new IllegalParameterException(
                    "end was lower than 0, selections have to be positive (was " + end + ").");
        }
        if (end - start < 0) {
            throw new IllegalParameterException(
                    "calculation (start - end) was lower than 0, selections have to cover a positive text range (provided indexes: "
                            + start + ", " + end + ").");
        }
        this.start = start;
        this.end = end;
        this.resourceUri = null;
        this.score = null;
    }

    public TextPositionSelector(int start, int end, float score) {
        this(start, end);
        this.score = score;
    }

    public TextPositionSelector(int start, int end, URI resourceUri, float score) {
        this(start, end, score);
        this.resourceUri = resourceUri;
    }

    public TextPositionSelector(int start, int end, String resourceUri, float score) throws URISyntaxException {
        this(start, end, new URI(resourceUri), score);
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    public URI getResourceUri() {
        return this.resourceUri;
    }

    public Float getScore() {
        return this.score;
    }
}
