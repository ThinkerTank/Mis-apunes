package co.edu.icesi.sgmms.model.structures;

/** Runs the whole graph contract on the adjacency-list representation. */
class AdjacencyListGraphTest extends GraphContractTest {

    @Override
    protected IGraph<String> newGraph() {
        return new AdjacencyListGraph<>();
    }
}
