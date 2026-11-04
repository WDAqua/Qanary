package eu.wdaqua.qanary.web;

import static eu.wdaqua.qanary.web.LogSanitizer.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    @Test
    void lineBreaksCannotForgeLogEntries() {
        assertEquals("question  ERROR forged entry", sanitize("question\r\nERROR forged entry"));
    }

    @Test
    void tabsAreReplaced() {
        assertEquals("a b", sanitize("a\tb"));
    }

    @Test
    void valuesWithoutLineBreaksAreLoggedAsBefore() {
        assertEquals("What is the capital of Germany?", sanitize("What is the capital of Germany?"));
        assertEquals("[first, second]", sanitize(List.of("first", "second")));
        assertEquals("null", sanitize(null));
    }

    /**
     * a traced call would add annotations to the process graph for every log
     * message
     */
    @Test
    void isNotTracedByTheQanaryAspect() {
        assertFalse(Arrays.stream(LogSanitizer.class.getDeclaredFields()).map(Field::getName)
                .anyMatch(name -> name.startsWith("ajc$")));
    }
}
