package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Runs the whole graph contract on the adjacency-matrix representation. */
class AdjacencyMatrixGraphTest extends GraphContractTest {

    @Override
    protected IGraph<String> newGraph() {
        return new AdjacencyMatrixGraph<>();
    }

    @Test
    void matrixGrowsBeyondItsInitialCapacityAndKeepsEdges() {
        IGraph<Integer> g = new AdjacencyMatrixGraph<>();
        for (int i = 0; i < 40; i++) {
            g.addVertex(i);
            if (i > 0) {
                g.addEdge(i - 1, i, 1);
            }
        }

        assertEquals(39, g.edgeCount());
        assertEquals(39, g.dijkstra(0).distanceTo(39), 1e-9);
    }
}
