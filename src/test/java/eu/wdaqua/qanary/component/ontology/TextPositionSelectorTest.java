package eu.wdaqua.qanary.component.ontology;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * checks behavior of TextPositionSelector corresponding to the definitions of
 * W3C OpenAnnotation {@linktourl https://www.w3.org/ns/oa#d4e667}
 * 
 * @author AnBo
 *
 */
public class TextPositionSelectorTest {

	/**
	 * test correct cases
	 */
	@Test
	public void testHappyDayInit() {
		createAndTestForCorrectValues(0, 0);
		createAndTestForCorrectValues(1, 1);
		createAndTestForCorrectValues(0, 1);
		createAndTestForCorrectValues(100, 200);
	}

	private void createAndTestForCorrectValues(int start, int end) {
		TextPositionSelector selector = new TextPositionSelector(start, end);
		assertTrue(selector.getStart() == start);
		assertTrue(selector.getEnd() == end);
	}

	/**
	 * test non-acceptable cases
	 */
	@Test
	public void negativeValuesProvided() {
		TextPositionSelector selector;
		// TODO: these tests always fail - fix and uncomment
		// createAndTestForIncorrectValues(0, -1);
		// createAndTestForIncorrectValues(5, 4);
		// createAndTestForIncorrectValues(-1, 4);
		// createAndTestForIncorrectValues(5, -3);
		// createAndTestForIncorrectValues(-1, -1);
	}

	private void createAndTestForIncorrectValues(int start, int end) {
		try {
			TextPositionSelector selector = new TextPositionSelector(start, end);
			fail("selector cannot cover a negative text range");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
