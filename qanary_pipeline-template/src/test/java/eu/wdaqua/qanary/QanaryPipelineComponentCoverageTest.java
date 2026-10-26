package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * A pipeline acting as component of a parent pipeline runs its own,
 * pre-configured components on the parent's question and copies the resulting
 * annotations into the parent's process graph, credited to itself.
 */
class QanaryPipelineComponentCoverageTest {

    static final URI PARENT_ENDPOINT = URI.create("http://127.0.0.1:8081/sparql");
    static final URI PARENT_GRAPH = URI.create("urn:graph:parent-process");
    static final String QUESTION = "http://127.0.0.1:8081/question/stored-question__text_42";
    static final String OA = "http://www.w3.org/ns/openannotation/core/";

    private QanaryTripleStoreProxy store;
    private QanaryQuestionAnsweringController innerPipeline;
    private QanaryPipelineComponent component;

    @BeforeEach
    void setUp() throws Exception {
        // the parent's process graph knows the current question
        QanaryTripleStoreConnectorInMemory parentData = new QanaryTripleStoreConnectorInMemory();
        parentData.update("INSERT DATA { GRAPH <" + PARENT_GRAPH + "> { <" + QUESTION
                + "> <http://www.w3.org/2002/07/owl#sameAs> <urn:qanary:currentQuestion> } }");

        store = mock(QanaryTripleStoreProxy.class);
        when(store.select(anyString())).thenAnswer(call -> parentData.select(call.getArgument(0)));

        QanaryConfigurator configurator = mock(QanaryConfigurator.class);
        when(configurator.getQanaryTripleStoreConnector()).thenReturn(store);
        when(configurator.getEndpoint()).thenReturn(URI.create("http://127.0.0.1:8080/sparql"));

        innerPipeline = mock(QanaryQuestionAnsweringController.class);
        component = new QanaryPipelineComponent();
        component.setupTripleStoreConnector(configurator);
        ReflectionTestUtils.setField(component, "QANARY_COMPONENTS", List.of("NED-Comp", "QB-Comp"));
        ReflectionTestUtils.setField(component, "qanaryQuestionAnsweringController", innerPipeline);
        ReflectionTestUtils.setField(component, "env",
                new MockEnvironment().withProperty("spring.application.name", "PipelineAsComponent"));
    }

    @Test
    void processRunsTheInnerPipelineAndCopiesItsAnnotationsToTheParentGraph() throws Exception {
        // what the inner pipeline's components annotated in the sub graph
        Model innerAnnotations = ModelFactory.createDefaultModel();
        Resource annotation = innerAnnotations.createResource("urn:annotation:inner-1");
        annotation.addProperty(innerAnnotations.createProperty(OA, "hasBody"), "Leipzig");
        annotation.addProperty(innerAnnotations.createProperty(OA, "annotatedBy"),
                innerAnnotations.createResource("urn:qanary:PipelineAsComponent"));
        when(store.construct(anyString(), any(URI.class))).thenReturn(innerAnnotations);

        QanaryMessage parentMessage = new QanaryMessage(PARENT_ENDPOINT, PARENT_GRAPH);
        QanaryMessage result = component.process(parentMessage);

        assertSame(parentMessage, result);
        // the parent endpoint becomes the internal store of this run
        verify(store).setInternalConnector(PARENT_ENDPOINT, "PipelineAsComponent");
        verify(store).setInternEndpointGraph(PARENT_GRAPH);

        // the inner pipeline runs the configured components on the parent's question in its own sub graph
        ArgumentCaptor<QanaryMessage> innerMessage = ArgumentCaptor.forClass(QanaryMessage.class);
        verify(innerPipeline).executeComponentList(eq(URI.create(QUESTION)), eq(List.of("NED-Comp", "QB-Comp")),
                innerMessage.capture());
        URI subGraph = innerMessage.getValue().getInGraph();
        assertNotEquals(PARENT_GRAPH, subGraph);
        assertTrue(subGraph.toString().startsWith("urn:graph:"));

        // the annotations constructed from the sub graph are inserted into the parent graph
        ArgumentCaptor<String> construct = ArgumentCaptor.forClass(String.class);
        verify(store).construct(construct.capture(), eq(subGraph));
        assertTrue(construct.getValue().contains("<urn:qanary:PipelineAsComponent>"), construct.getValue());
        ArgumentCaptor<String> insert = ArgumentCaptor.forClass(String.class);
        verify(store).updateInternal(insert.capture());
        assertTrue(insert.getValue().contains("GRAPH <" + PARENT_GRAPH + ">"), insert.getValue());
        assertTrue(insert.getValue().contains("urn:annotation:inner-1"), insert.getValue());
        assertTrue(insert.getValue().contains("\"Leipzig\""), insert.getValue());
        verify(store).clearEndpointGraphs();
    }

    @Test
    void constructedInsertQueryTargetsTheGivenGraph() {
        Model triples = ModelFactory.createDefaultModel();
        triples.createResource("urn:annotation:2").addProperty(triples.createProperty(OA, "hasTarget"),
                triples.createResource(QUESTION));

        String query = component.constructQueryFromModel(triples, PARENT_GRAPH, "/insert_constructed_triples.rq");

        assertTrue(query.contains("INSERT"), query);
        assertTrue(query.contains("GRAPH <" + PARENT_GRAPH + ">"), query);
        assertTrue(query.contains("<urn:annotation:2>") && query.contains("<" + QUESTION + ">"), query);
    }

    @Test
    void constructedInsertQueryIsEmptyIfTheQueryTemplateIsMissing() {
        assertEquals("", component.constructQueryFromModel(ModelFactory.createDefaultModel(), PARENT_GRAPH,
                "/no-such-template.rq"));
    }
}
