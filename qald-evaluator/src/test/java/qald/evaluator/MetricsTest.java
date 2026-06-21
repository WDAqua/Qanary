package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.qald.evaluator.evaluation.Metrics;

class MetricsTest {

    private static final double DELTA = 1e-9;

    @Test
    void gettersAndSettersRoundTrip() {
        Metrics m = new Metrics();
        m.setPrecision(0.5);
        m.setRecall(0.25);
        m.setfMeasure(0.75);
        assertEquals(0.5, m.getPrecision(), DELTA);
        assertEquals(0.25, m.getRecall(), DELTA);
        assertEquals(0.75, m.getfMeasure(), DELTA);
    }

    @Test
    void bothEmptyIsPerfectScore() {
        Metrics m = new Metrics();
        m.compute(Collections.emptyList(), Collections.emptyList());
        assertEquals(1.0, m.getPrecision(), DELTA);
        assertEquals(1.0, m.getRecall(), DELTA);
        assertEquals(1.0, m.getfMeasure(), DELTA);
    }

    @Test
    void noExpectedButSystemAnswersIsZeroScore() {
        Metrics m = new Metrics();
        m.compute(Collections.emptyList(), Arrays.asList("a"));
        assertEquals(0.0, m.getPrecision(), DELTA);
        assertEquals(0.0, m.getRecall(), DELTA);
        assertEquals(0.0, m.getfMeasure(), DELTA);
    }

    @Test
    void expectedButNoSystemAnswersHasZeroRecall() {
        Metrics m = new Metrics();
        m.compute(Arrays.asList("a", "b"), Collections.emptyList());
        // precision is set to 1.0 in this branch, recall to 0.0
        assertEquals(1.0, m.getPrecision(), DELTA);
        assertEquals(0.0, m.getRecall(), DELTA);
        // precision != 0, recall == 0 -> fMeasure computed as 0
        assertEquals(0.0, m.getfMeasure(), DELTA);
    }

    @Test
    void perfectMatchYieldsFullScores() {
        Metrics m = new Metrics();
        List<String> answers = Arrays.asList("a", "b");
        m.compute(answers, answers);
        assertEquals(1.0, m.getPrecision(), DELTA);
        assertEquals(1.0, m.getRecall(), DELTA);
        assertEquals(1.0, m.getfMeasure(), DELTA);
    }

    @Test
    void partialMatchComputesPrecisionRecallAndFMeasure() {
        Metrics m = new Metrics();
        // expected {a,b,c}; system {a,b,x}: 2 correct of 3 system, 2 of 3 expected
        m.compute(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "x"));
        assertEquals(2.0 / 3.0, m.getPrecision(), DELTA);
        assertEquals(2.0 / 3.0, m.getRecall(), DELTA);
        double expectedF = (2 * (2.0 / 3.0) * (2.0 / 3.0)) / ((2.0 / 3.0) + (2.0 / 3.0));
        assertEquals(expectedF, m.getfMeasure(), DELTA);
    }

    @Test
    void noOverlapGivesZeroFMeasure() {
        Metrics m = new Metrics();
        m.compute(Arrays.asList("a", "b"), Arrays.asList("x", "y"));
        assertEquals(0.0, m.getPrecision(), DELTA);
        assertEquals(0.0, m.getRecall(), DELTA);
        assertEquals(0.0, m.getfMeasure(), DELTA);
    }
}
