package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.VertexNotFoundException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests shared by both graph representations. Expected values were computed by hand on this
 * graph:
 *
 * <pre>
 *   A -4- B      A -1- C      C -2- B      B -5- D
 *   C -8- D      C -10- E     D -2- E
 * </pre>
 */
abstract class GraphContractTest {

    private static final double EPS = 1e-9;

    protected abstract IGraph<String> newGraph();

    private IGraph<String> handGraph() {
        IGraph<String> g = newGraph();
        for (String v : new String[] {"A", "B", "C", "D", "E"}) {
            g.addVertex(v);
        }
        g.addEdge("A", "B", 4);
        g.addEdge("A", "C", 1);
        g.addEdge("C", "B", 2);
        g.addEdge("B", "D", 5);
        g.addEdge("C", "D", 8);
        g.addEdge("C", "E", 10);
        g.addEdge("D", "E", 2);
        return g;
    }

    private IGraph<String> disconnectedGraph() {
        IGraph<String> g = newGraph();
        for (String v : new String[] {"A", "B", "C", "D", "E"}) {
            g.addVertex(v);
        }
        g.addEdge("A", "B", 1);
        g.addEdge("C", "D", 1);
        return g;
    }

    // ------------------------------------------------------------ structure

    @Test
    void basicOperationsKeepCountsConsistent() {
        IGraph<String> g = handGraph();

        assertEquals(5, g.vertexCount());
        assertEquals(7, g.edgeCount());
        assertFalse(g.addVertex("A"));
        assertEquals(4, g.getWeight("A", "B"), EPS);
        assertEquals(Double.POSITIVE_INFINITY, g.getWeight("A", "E"));
        assertEquals(3, g.getAdjacent("B").size());
        assertEquals(7, g.edges().size());
    }

    @Test
    void addingAnExistingEdgeReplacesItsWeight() {
        IGraph<String> g = handGraph();
        g.addEdge("A", "B", 9);

        assertEquals(7, g.edgeCount());
        assertEquals(9, g.getWeight("B", "A"), EPS);
    }

    @Test
    void removeEdgeAndUpdateWeight() {
        IGraph<String> g = handGraph();

        assertTrue(g.removeEdge("C", "E"));
        assertFalse(g.removeEdge("C", "E"));
        assertFalse(g.hasEdge("E", "C"));
        assertEquals(6, g.edgeCount());

        g.updateWeight("D", "E", 7);
        assertEquals(7, g.getWeight("E", "D"), EPS);
        assertThrows(IllegalArgumentException.class, () -> g.updateWeight("A", "E", 1));
    }

    @Test
    void invalidEdgesAndUnknownVerticesAreRejected() {
        IGraph<String> g = handGraph();

        assertThrows(IllegalArgumentException.class, () -> g.addEdge("A", "B", -1));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge("A", "A", 1));
        assertThrows(VertexNotFoundException.class, () -> g.addEdge("A", "Z", 1));
        assertThrows(VertexNotFoundException.class, () -> g.bfs("Z"));
    }

    // ------------------------------------------------------------ BFS

    @Test
    void bfsComputesHopsOnTheHandGraph() {
        BfsResult<String> result = handGraph().bfs("A");

        assertEquals(0, result.hopsTo("A"));
        assertEquals(1, result.hopsTo("B"));
        assertEquals(1, result.hopsTo("C"));
        assertEquals(2, result.hopsTo("D"));
        assertEquals(2, result.hopsTo("E"));
        assertEquals(5, result.reachableCount());
        assertTrue(result.unreachable().isEmpty());
        assertEquals(3, result.pathTo("E").size());
    }

    @Test
    void bfsReportsUnreachableVerticesOnADisconnectedGraph() {
        BfsResult<String> result = disconnectedGraph().bfs("A");

        assertTrue(result.isReachable("B"));
        assertFalse(result.isReachable("C"));
        assertEquals(-1, result.hopsTo("D"));
        assertEquals("[C, D, E]", result.unreachable().toString());
        assertTrue(result.pathTo("D").isEmpty());
    }

    // ------------------------------------------------------------ DFS

    @Test
    void dfsFindsEveryConnectedComponent() {
        DfsResult<String> result = disconnectedGraph().dfs();

        assertEquals(3, result.componentCount());
        assertTrue(result.sameComponent("A", "B"));
        assertTrue(result.sameComponent("C", "D"));
        assertFalse(result.sameComponent("A", "C"));
        assertFalse(result.hasCycle());
    }

    @Test
    void dfsDetectsTheCycleClosedByANewEdge() {
        IGraph<String> path = newGraph();
        for (String v : new String[] {"A", "B", "C"}) {
            path.addVertex(v);
        }
        path.addEdge("A", "B", 1);
        path.addEdge("B", "C", 1);
        assertFalse(path.dfs().hasCycle());

        path.addEdge("C", "A", 1);
        DfsResult<String> result = path.dfs();
        assertTrue(result.hasCycle());
        assertTrue(result.cycleVertices().contains("A"));
        assertTrue(result.cycleVertices().contains("C"));
    }

    // ------------------------------------------------------------ Dijkstra

    @Test
    void dijkstraMatchesTheHandComputedDistancesAndPath() {
        ShortestPathResult<String> result = handGraph().dijkstra("A");

        assertEquals(0, result.distanceTo("A"), EPS);
        assertEquals(1, result.distanceTo("C"), EPS);
        assertEquals(3, result.distanceTo("B"), EPS);
        assertEquals(8, result.distanceTo("D"), EPS);
        assertEquals(10, result.distanceTo("E"), EPS);
        assertEquals("[A, C, B, D, E]", result.pathTo("E").toString());
    }

    @Test
    void dijkstraGivesInfiniteDistanceToAnUnreachableVertex() {
        ShortestPathResult<String> result = disconnectedGraph().dijkstra("A");

        assertEquals(Double.POSITIVE_INFINITY, result.distanceTo("D"));
        assertFalse(result.isReachable("E"));
        assertTrue(result.pathTo("D").isEmpty());
    }

    // ------------------------------------------------------------ Floyd-Warshall

    @Test
    void floydWarshallMatchesTheHandGraph() {
        AllPairsResult<String> result = handGraph().floydWarshall();

        assertEquals(10, result.distance("A", "E"), EPS);
        assertEquals(10, result.distance("E", "A"), EPS);
        assertEquals(7, result.distance("C", "D"), EPS);
        assertEquals("[A, C, B, D, E]", result.path("A", "E").toString());
        assertEquals(Double.POSITIVE_INFINITY, disconnectedGraph().floydWarshall().distance("A", "C"));
    }

    @Test
    void floydWarshallAgreesWithDijkstraOnARandomGraph() {
        IGraph<Integer> g = newIntGraph();
        Random random = new Random(2026);
        int n = 30;
        for (int i = 0; i < n; i++) {
            g.addVertex(i);
        }
        for (int i = 0; i < 80; i++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            if (a != b) {
                g.addEdge(a, b, 1 + random.nextInt(20));
            }
        }
        AllPairsResult<Integer> all = g.floydWarshall();
        for (int source = 0; source < n; source += 3) {
            ShortestPathResult<Integer> single = g.dijkstra(source);
            for (int target = 0; target < n; target++) {
                assertEquals(single.distanceTo(target), all.distance(source, target), EPS,
                        "pair " + source + " -> " + target);
            }
        }
    }

    // ------------------------------------------------------------ MST

    @Test
    void primAndKruskalProduceTheHandComputedTree() {
        IGraph<String> g = handGraph();
        MstResult<String> prim = g.primMST("A");
        MstResult<String> kruskal = g.kruskalMST();

        assertEquals(10, prim.getTotalWeight(), EPS);
        assertEquals(10, kruskal.getTotalWeight(), EPS);
        assertTrue(prim.spansAllVertices());
        assertTrue(kruskal.spansAllVertices());
        assertTrue(kruskal.containsEdge("B", "D"));
        assertFalse(kruskal.containsEdge("A", "B"));
    }

    @Test
    void primAndKruskalAgreeOnCostWhenWeightsTie() {
        IGraph<String> square = newGraph();
        for (String v : new String[] {"A", "B", "C", "D"}) {
            square.addVertex(v);
        }
        square.addEdge("A", "B", 2);
        square.addEdge("B", "C", 2);
        square.addEdge("C", "D", 2);
        square.addEdge("D", "A", 2);
        square.addEdge("A", "C", 2);

        assertEquals(6, square.primMST("C").getTotalWeight(), EPS);
        assertEquals(6, square.kruskalMST().getTotalWeight(), EPS);
        assertEquals(3, square.kruskalMST().getEdges().size());
    }

    @Test
    void kruskalBuildsAForestOnADisconnectedGraph() {
        MstResult<String> forest = disconnectedGraph().kruskalMST();

        assertEquals(2, forest.getEdges().size());
        assertFalse(forest.spansAllVertices());
    }

    @SuppressWarnings("unchecked")
    private IGraph<Integer> newIntGraph() {
        return (IGraph<Integer>) (IGraph<?>) newGraph();
    }
}
