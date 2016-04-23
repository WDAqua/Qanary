package eu.wdaqua.qanary.component.ontology;

/**
 * represents the textpositionselector concept of the W3C OpenAnnotation
 * vocabulary
 * 
 * @author AnBo
 *
 */
public class TextPositionSelector {
	private final int start;
	private final int end;

	public TextPositionSelector(int start, int end) {
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
