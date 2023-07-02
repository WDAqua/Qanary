package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * test behavior of QanaryTripleStoreConnectorInMemory bean
 * 
 * @author AnBo-de
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = QanaryTripleStoreConnectorInMemoryTest.class)
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class, classes = {
		QanaryTripleStoreConnectorInMemory.class })
class QanaryTripleStoreConnectorInMemoryTest {

	static {
		System.setProperty("enable.in-memory-triplestore", "true");
	}

	@Autowired
	QanaryTripleStoreConnectorInMemory myQanaryTripleStoreConnectorInMemory;

	/**
	 * test if environment parameter enables the QanaryTripleStoreConnectorInMemory
	 * bean
	 */
	@Test
	void testInit() {
		assertNotNull(myQanaryTripleStoreConnectorInMemory);
	}

	@Test
	void testAskInsertDeleteQueries() throws SparqlQueryFailed, URISyntaxException, IOException {
		URI graph = new URI("urn:test");
		String triple = "<urn:s> <urn:p> <urn:o>";

		myQanaryTripleStoreConnectorInMemory.connect();

		// there should be nothing in the triplestore
		checkForExistingTriples(false);
		assertEquals(0, getNumberOfAvailableTriples());

		// insert 1 triple
		String insertQuery = "INSERT DATA { GRAPH <" + graph.toASCIIString() + "> { " + triple + " . } }";

		// check the presence of the triples
		myQanaryTripleStoreConnectorInMemory.update(insertQuery);
		checkForExistingTriples(true);
		assertEquals(1, getNumberOfAvailableTriples());

		// delete added triple
		String deleteQuery = "DELETE DATA { GRAPH <" + graph.toASCIIString() + "> { " + triple + " . } }";
		myQanaryTripleStoreConnectorInMemory.update(deleteQuery);

		// there should be nothing in the triplestore anymore
		checkForExistingTriples(false);
		assertEquals(0, getNumberOfAvailableTriples());
	}
	
	@Test
	void testResetBehavior() throws URISyntaxException, SparqlQueryFailed, IOException {
		URI graph = new URI("urn:test");
		String triple = "<urn:s> <urn:p> <urn:o>";

		myQanaryTripleStoreConnectorInMemory.connect();

		// there should be nothing in the triplestore
		checkForExistingTriples(false);
		assertEquals(0, getNumberOfAvailableTriples());

		// insert 1 triple
		String insertQuery = "INSERT DATA { GRAPH <" + graph.toASCIIString() + "> { " + triple + " . } }";

		// check the presence of the triples
		myQanaryTripleStoreConnectorInMemory.update(insertQuery);
		checkForExistingTriples(true);
		assertEquals(1, getNumberOfAvailableTriples());

		// the in-memory store should be reseted
		myQanaryTripleStoreConnectorInMemory.connect(); 
		
		// there should be nothing in the triplestore anymore
		checkForExistingTriples(false);
		assertEquals(0, getNumberOfAvailableTriples());
	}

	/**
	 * checks if triples are available or not depending on given boolean parameter
	 * 
	 * @param isExpectingTriples
	 * @throws SparqlQueryFailed
	 */
	void checkForExistingTriples(boolean isExpectingTriples) throws SparqlQueryFailed {
		assertEquals(isExpectingTriples, myQanaryTripleStoreConnectorInMemory.ask("ASK { graph ?g { ?s ?p ?o . } }"));
	}

	private int getNumberOfAvailableTriples() throws SparqlQueryFailed, IOException {
		String storedQuery = "/queries/select_count_all_triples.rq";
		ResultSet resultSet = myQanaryTripleStoreConnectorInMemory
				.select(QanaryTripleStoreConnector.readFileFromResources(storedQuery));
		while (resultSet.hasNext()) {
			return resultSet.nextSolution().get("count").asLiteral().getInt();
		}
		throw new SparqlQueryFailed(storedQuery, QanaryTripleStoreConnectorInMemory.class.getCanonicalName(),
				new Exception("no rows returned"));
	}

}
