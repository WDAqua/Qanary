package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

/**
 * The embedded frontend starts a question answering run with the components
 * configured as default for the pipeline.
 */
class QanaryEmbeddedQaWebFrontendControllerCoverageTest {

    @Test
    void postedQuestionStartsARunWithTheDefaultComponents() throws Exception {
        QanaryConfigurator configurator = mock(QanaryConfigurator.class);
        when(configurator.getDefaultComponentNames()).thenReturn(List.of("NED-DBpediaSpotlight", "QB-Simple"));
        when(configurator.getDefaultComponentNamesAsString()).thenReturn("NED-DBpediaSpotlight,QB-Simple");
        when(configurator.getEndpoint()).thenReturn(URI.create("http://127.0.0.1:8080/sparql"));
        QanaryQuestionAnsweringRun run = new QanaryQuestionAnsweringRun(
                URI.create("http://127.0.0.1:8080/question/stored-question__text_7"),
                URI.create("http://127.0.0.1:8080/sparql"), URI.create("urn:graph:run-7"),
                URI.create("urn:graph:run-7"), configurator);
        QanaryQuestionAnsweringController questionAnswering = mock(QanaryQuestionAnsweringController.class);
        when(questionAnswering.startquestionansweringwithtextquestion(eq("What is the capital of Saxony?"),
                eq(List.of("NED-DBpediaSpotlight", "QB-Simple")), isNull(), isNull(), isNull(), isNull()))
                .thenAnswer(call -> new ResponseEntity<>(run, HttpStatus.CREATED));
        QanaryEmbeddedQaWebFrontendController controller = new QanaryEmbeddedQaWebFrontendController(configurator,
                mock(QanaryQuestionController.class), questionAnswering);
        ExtendedModelMap model = new ExtendedModelMap();

        // Known defect (reported, not fixed here): reading the answer back fails in the
        // QanaryQuestion(URI, QanaryConfigurator) constructor of qanary_commons, which asks for
        // the triple store connector before it has stored the configurator. Once that is fixed,
        // this test should check the rendered view ("qa_output" or "qa_no-answer") instead.
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> controller.qa("What is the capital of Saxony?", model));

        assertEquals("in QanaryQuestion a QanaryTripleStoreConnector instance is required", failure.getMessage());
        assertEquals("What is the capital of Saxony?", model.getAttribute("question"));
    }

    @Test
    void frontendAllowsRequestsFromOtherOrigins() {
        QanaryConfigurator configurator = mock(QanaryConfigurator.class);
        QanaryEmbeddedQaWebFrontendController controller = new QanaryEmbeddedQaWebFrontendController(configurator,
                mock(QanaryQuestionController.class), mock(QanaryQuestionAnsweringController.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.setVaryResponseHeader(response);

        assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("forward:/qanary-ui/index.html", controller.qa());
    }
}
