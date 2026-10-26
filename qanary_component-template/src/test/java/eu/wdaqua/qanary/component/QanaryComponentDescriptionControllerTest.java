package eu.wdaqua.qanary.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests of {@link QanaryComponentDescriptionController}: loading the component's
 * Turtle self-description, serving it in every supported RDF format, and staying
 * usable when no description is available.
 */
class QanaryComponentDescriptionControllerTest {

    private static final String COMPONENT = "urn:qanary:test-component";
    private static final String DESCRIPTION = """
            @prefix qa: <http://www.wdaqua.eu/qa#> .
            <urn:qanary:test-component> a qa:QanaryComponent ;
                <http://www.w3.org/2000/01/rdf-schema#label> "test component" .
            """;

    private static QanaryComponentDescriptionController controllerFor(Resource resource) throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getResource("classpath:component-description.ttl")).thenReturn(resource);
        return new QanaryComponentDescriptionController(context);
    }

    private static QanaryComponentDescriptionController controllerWithDescription(Path dir) throws Exception {
        Path file = dir.resolve("component-description.ttl");
        Files.writeString(file, DESCRIPTION);
        return controllerFor(new FileSystemResource(file));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", QanaryConfiguration.rdfcomponentdescription);
        request.addHeader("Accept", "text/turtle");
        request.addHeader("User-Agent", "unit-test");
        return request;
    }

    @Test
    void loadsTheTurtleDescriptionFromTheClasspathResource(@TempDir Path dir) throws Exception {
        QanaryComponentDescriptionController controller = controllerWithDescription(dir);

        assertNotNull(controller.model);
        assertEquals(2, controller.model.size());
    }

    @Test
    void servesTheDescriptionInEveryRdfFormat(@TempDir Path dir) throws Exception {
        QanaryComponentDescriptionController controller = controllerWithDescription(dir);
        HttpServletRequest request = request();

        String turtle = controller.getModelAsTurtle(request);
        assertTrue(turtle.contains("<" + COMPONENT + ">") && turtle.contains("\"test component\""), turtle);

        String nTriples = controller.getModelAsNTriples(request);
        assertTrue(nTriples.contains("<" + COMPONENT + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
                + "<http://www.wdaqua.eu/qa#QanaryComponent> ."), nTriples);
        assertEquals(2, nTriples.lines().filter(line -> !line.isBlank()).count());

        assertTrue(controller.getModelAsN3(request).contains(COMPONENT));
        String xml = controller.getModelAsXml(request);
        assertTrue(xml.contains("rdf:RDF") && xml.contains("rdf:about=\"" + COMPONENT + "\""), xml);
        String json = controller.getModelAsJson(request);
        assertTrue(json.contains("\"" + COMPONENT + "\"") && json.contains("QanaryComponent"), json);
    }

    @Test
    void aMissingDescriptionFileLeavesEveryFormatEmpty(@TempDir Path dir) throws Exception {
        QanaryComponentDescriptionController controller = controllerFor(new FileSystemResource(dir.resolve("missing.ttl")));

        assertNull(controller.model);
        assertEquals("", controller.getModelAsTurtle(request()));
        assertEquals("", controller.getModelAsJson(request()));
    }

    @Test
    void fallsBackToTheSourceTreeWhenTheClasspathLookupFails() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getResource("classpath:component-description.ttl")).thenThrow(new IllegalStateException("no classpath"));

        // the controller falls back to src/main/resources/ (relative to the module) and must
        // start in either case; this module currently ships no description file there
        QanaryComponentDescriptionController controller = new QanaryComponentDescriptionController(context);

        if (new java.io.File("src/main/resources/component-description.ttl").exists()) {
            assertNotNull(controller.model);
        } else {
            assertNull(controller.model);
            assertEquals("", controller.getModelAsNTriples(request()));
        }
    }

    @Test
    void descriptionPageIsTheConfiguredTemplate(@TempDir Path dir) throws Exception {
        QanaryComponentDescriptionController controller = controllerWithDescription(dir);

        assertEquals(QanaryConfiguration.description_file, controller.description(new MockHttpServletResponse()));
    }

    @Test
    void requestsWithoutHeaderNamesAreStillServed(@TempDir Path dir) throws Exception {
        QanaryComponentDescriptionController controller = controllerWithDescription(dir);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenReturn(null);

        assertTrue(controller.getModelAsTurtle(request).contains(COMPONENT));
    }
}
