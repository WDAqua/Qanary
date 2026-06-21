package eu.wdaqua.qanary.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QanaryExplanationDataTest {

    @Test
    void defaultConstructorLeavesFieldsNull() {
        QanaryExplanationData data = new QanaryExplanationData();
        assertNull(data.getGraph());
        assertNull(data.getQuestionId());
        assertNull(data.getServerHost());
        assertNull(data.getComponent());
        assertNull(data.getExplanations());
    }

    @Test
    void threeArgConstructorStoresGraphQuestionAndHost() {
        QanaryExplanationData data =
                new QanaryExplanationData("urn:graph", "q1", "http://host");
        assertEquals("urn:graph", data.getGraph());
        assertEquals("q1", data.getQuestionId());
        assertEquals("http://host", data.getServerHost());
    }

    @Test
    void settersRoundTrip() {
        QanaryExplanationData data = new QanaryExplanationData();
        data.setGraph("g");
        data.setQuestionId("q");
        data.setServerHost("h");
        data.setComponent("NER");
        Map<String, String> explanations = new HashMap<>();
        explanations.put("text", "because");
        data.setExplanations(explanations);

        assertEquals("g", data.getGraph());
        assertEquals("q", data.getQuestionId());
        assertEquals("h", data.getServerHost());
        assertEquals("NER", data.getComponent());
        assertEquals("because", data.getExplanations().get("text"));
    }
}
