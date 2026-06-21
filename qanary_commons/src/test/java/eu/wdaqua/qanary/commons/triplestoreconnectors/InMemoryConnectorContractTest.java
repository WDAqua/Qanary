package eu.wdaqua.qanary.commons.triplestoreconnectors;

/**
 * Runs the {@link AbstractQanaryTripleStoreConnectorContract} against the in-memory
 * Jena connector (no Docker required).
 */
class InMemoryConnectorContractTest extends AbstractQanaryTripleStoreConnectorContract {

    private static final QanaryTripleStoreConnectorInMemory CONNECTOR =
            new QanaryTripleStoreConnectorInMemory();

    @Override
    protected QanaryTripleStoreConnector connector() {
        return CONNECTOR;
    }
}
