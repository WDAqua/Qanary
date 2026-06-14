package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.QuerySolution;
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
	 * SELECT must return the exact resources that were inserted. Guards the
	 * Jena 5 query engine + in-memory dataset (DatasetFactory.create, replacing
	 * the removed TDB1 TDBFactory.createDataset) against a regression.
	 */
	@Test
	void testSelectReturnsInsertedValues() throws URISyntaxException, SparqlQueryFailed {
		URI graph = new URI("urn:test");
		myQanaryTripleStoreConnectorInMemory.connect();

		myQanaryTripleStoreConnectorInMemory.update(
				"INSERT DATA { GRAPH <" + graph.toASCIIString() + "> { <urn:s> <urn:p> <urn:o> . } }");

		ResultSet resultSet = myQanaryTripleStoreConnectorInMemory
				.select("SELECT ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o . } }");
		assertTrue(resultSet.hasNext(), "expected exactly one result row");
		QuerySolution solution = resultSet.nextSolution();
		assertEquals("urn:s", solution.getResource("s").getURI());
		assertEquals("urn:p", solution.getResource("p").getURI());
		assertEquals("urn:o", solution.getResource("o").getURI());
		assertFalse(resultSet.hasNext(), "expected exactly one result row");
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
