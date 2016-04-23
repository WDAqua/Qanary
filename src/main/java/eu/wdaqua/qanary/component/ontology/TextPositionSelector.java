package eu.wdaqua.qanary.component.ontology;

import org.apache.jena.reasoner.IllegalParameterException;

/**
 * represents the textpositionselector concept of the W3C OpenAnnotation
 * vocabulary {@linktourl https://www.w3.org/ns/oa#d4e667}
 * 
 * @author AnBo
 *
 */
public class TextPositionSelector {
	private final int start;
	private final int end;

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
	}

	public int getStart() {
		return this.start;
	}

	public int getEnd() {
		return this.end;
	}
}
