package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorVirtuoso;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;

import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QanaryPipelineComponentTest {

    protected ClassLoader classLoader = this.getClass().getClassLoader();
    @MockBean
    QanaryTripleStoreConnectorVirtuoso qanaryTripleStoreConnectorVirtuoso;
    @InjectMocks
    QanaryPipelineComponent qanaryPipelineComponent;
    URI testUri = new URI("testUri");

    public QanaryPipelineComponentTest() throws URISyntaxException {
    }

    @Before
    public void setup() {
        doNothing().when(qanaryTripleStoreConnectorVirtuoso).connect();
    }

    @Test
    public void constructQueryFromModelTest() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("annotation"),
                ResourceFactory.createProperty("oa:annotatedBy"),
                ResourceFactory.createPlainLiteral("test")
        ));
        String query = qanaryPipelineComponent.constructQueryFromModel(model, testUri, "/insert_constructed_triples.rq");
        String expectedResult = readFileFromTestResources("constructQueryFromModelTestResultQuery");

        Assertions.assertNotEquals(expectedResult, query);
    }
    
    private String readFileFromTestResources(String path) throws IOException {
        File file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

}