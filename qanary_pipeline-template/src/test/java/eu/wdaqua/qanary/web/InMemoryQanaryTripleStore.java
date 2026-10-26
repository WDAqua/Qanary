package eu.wdaqua.qanary.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.apache.jena.query.ResultSet;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Test double for the pipeline's {@link QanaryTripleStoreProxy}: a Mockito mock
 * whose SPARQL operations are executed by a real in-memory Jena triplestore
 * ({@link QanaryTripleStoreConnectorInMemory}). Tests can therefore assert the
 * triples the controllers actually write and read.
 * <p>
 * SPARQL {@code LOAD} requests (the Qanary question set-up loads ontologies from
 * the Web) are recorded but not executed, so the tests never leave the machine.
 * Individual updates or selects can be made to fail to test error handling.
 */
final class InMemoryQanaryTripleStore {

    private final QanaryTripleStoreConnectorInMemory store = new QanaryTripleStoreConnectorInMemory();
    private final QanaryTripleStoreProxy proxy = mock(QanaryTripleStoreProxy.class);
    private final List<String> executedUpdates = new CopyOnWriteArrayList<>();
    private final List<String> skippedLoads = new CopyOnWriteArrayList<>();
    private volatile Predicate<String> failingUpdates = query -> false;
    private volatile boolean failingSelects = false;

    InMemoryQanaryTripleStore() throws SparqlQueryFailed {
        doAnswer(invocation -> this.select(invocation.getArgument(0))).when(proxy).select(anyString());
        doAnswer(invocation -> this.ask(invocation.getArgument(0))).when(proxy).ask(anyString());
        doAnswer(invocation -> {
            this.update(invocation.getArgument(0));
            return null;
        }).when(proxy).update(anyString());
        when(proxy.getFullEndpointDescription()).thenReturn("in-memory test triplestore");
    }

    /**
     * the mocked proxy that is handed to the controllers
     */
    QanaryTripleStoreProxy proxy() {
        return proxy;
    }

    ResultSet select(String sparql) throws SparqlQueryFailed {
        if (failingSelects) {
            throw new SparqlQueryFailed(sparql, "in-memory test triplestore", new Exception("select disabled by test"));
        }
        return store.select(sparql);
    }

    boolean ask(String sparql) throws SparqlQueryFailed {
        return store.ask(sparql);
    }

    void update(String sparql) throws SparqlQueryFailed {
        if (failingUpdates.test(sparql)) {
            throw new SparqlQueryFailed(sparql, "in-memory test triplestore", new Exception("update rejected by test"));
        }
        if (sparql.trim().toUpperCase().startsWith("LOAD")) {
            skippedLoads.add(sparql);
            return;
        }
        store.update(sparql);
        executedUpdates.add(sparql);
    }

    /**
     * let every update for which the predicate is true fail with {@link SparqlQueryFailed}
     */
    void failUpdatesMatching(Predicate<String> predicate) {
        this.failingUpdates = predicate;
    }

    /**
     * let every SELECT query fail with {@link SparqlQueryFailed}
     */
    void failSelects() {
        this.failingSelects = true;
    }

    List<String> executedUpdates() {
        return executedUpdates;
    }

    List<String> skippedLoads() {
        return skippedLoads;
    }

    /**
     * true if the triple pattern matches in the given named graph
     */
    boolean contains(String graph, String triplePattern) throws SparqlQueryFailed {
        return store.ask("ASK { GRAPH <" + graph + "> { " + triplePattern + " } }");
    }
}
