package eu.wdaqua.qanary.explainability.aspects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MethodObjectTest {

    @Test
    void constructorStoresCallerMethodAndInput() {
        Object[] input = {"a", 1};
        MethodObject m = new MethodObject("caller", "doWork", input);
        assertEquals("caller", m.getCaller());
        assertEquals("doWork", m.getMethod());
        assertArrayEquals(input, m.getInput());
    }

    @Test
    void allSettersRoundTrip() {
        MethodObject m = new MethodObject("c", "method", new Object[] {});
        m.setCaller("c2");
        m.setMethod("m2");
        m.setInput(new Object[] {"x"});
        m.setOutput("out");
        m.setAnnotatedBy("annotator");
        m.setExplanationType("type");
        m.setExplanationValue("value");
        m.setDocstring("doc");
        m.setErrorOccurred(true);

        assertEquals("c2", m.getCaller());
        assertEquals("m2", m.getMethod());
        assertArrayEquals(new Object[] {"x"}, m.getInput());
        assertEquals("out", m.getOutput());
        assertEquals("annotator", m.getAnnotatedBy());
        assertEquals("type", m.getExplanationType());
        assertEquals("value", m.getExplanationValue());
        assertEquals("doc", m.getDocstring());
        assertTrue(m.isErrorOccurred());
    }

    @Test
    void errorOccurredDefaultsFalse() {
        MethodObject m = new MethodObject("c", "m", new Object[] {});
        assertFalse(m.isErrorOccurred());
    }

    @Test
    void toStringContainsKeyFields() {
        MethodObject m = new MethodObject("caller", "doWork", new Object[] {"in"});
        m.setOutput("result");
        m.setAnnotatedBy("annotator");
        String s = m.toString();
        assertTrue(s.contains("caller"));
        assertTrue(s.contains("doWork"));
        assertTrue(s.contains("result"));
        assertTrue(s.contains("annotator"));
    }
}
