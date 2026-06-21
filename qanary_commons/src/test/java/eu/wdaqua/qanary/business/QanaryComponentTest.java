package eu.wdaqua.qanary.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QanaryComponentTest {

    @Test
    void exposesNameUrlAndUsedFlag() {
        QanaryComponent c = new QanaryComponent("NER", "http://localhost:8080", true);
        assertEquals("NER", c.getName());
        assertEquals("http://localhost:8080", c.getUrl());
        assertTrue(c.isUsed());
    }

    @Test
    void usedFlagIsMutable() {
        QanaryComponent c = new QanaryComponent("NED", "http://localhost:8081", false);
        assertFalse(c.isUsed());
        c.setUsed(true);
        assertTrue(c.isUsed());
    }
}
