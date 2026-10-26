package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The reloadable property source reads an optional local properties file that
 * overrides the packaged configuration.
 */
class ReloadablePropertySourceCoverageTest {

    @Test
    void propertiesAreReadFromTheFile(@TempDir Path directory) throws Exception {
        Path file = Files.writeString(directory.resolve("application.local.properties"),
                "qanary.triplestore=http://127.0.0.1:8890/sparql\nqanary.components=NED,QB\n");

        ReloadablePropertySource source = new ReloadablePropertySource("local", file.toString());

        assertEquals("local", source.getName());
        assertEquals("http://127.0.0.1:8890/sparql", source.getProperty("qanary.triplestore"));
        assertNull(source.getProperty("server.port"));
    }

    @Test
    void sourceWithoutNameIsNamedAfterItsFile(@TempDir Path directory) throws Exception {
        Path file = Files.writeString(directory.resolve("override.properties"), "a=b\n");

        assertEquals(file.toString(), new ReloadablePropertySource("", file.toString()).getName());
    }

    @Test
    void missingFileBehavesLikeAnEmptySource(@TempDir Path directory) {
        ReloadablePropertySource source = new ReloadablePropertySource("local",
                directory.resolve("does-not-exist.properties").toString());

        assertNull(source.getProperty("qanary.triplestore"));
    }
}
