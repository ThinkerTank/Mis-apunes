package co.edu.icesi.sgmms.model.structures;

/**
 * Minimum spanning tree (or forest) produced by Prim or Kruskal.
 *
 * @param <T> vertex type
 */
public class MstResult<T> {

    private final String algorithm;
    private final LinkedList<Edge<T>> edges;
    private final double totalWeight;
    private final int vertexCount;

    MstResult(String algorithm, LinkedList<Edge<T>> edges, double totalWeight, int vertexCount) {
        this.algorithm = algorithm;
        this.edges = edges;
        this.totalWeight = totalWeight;
        this.vertexCount = vertexCount;
    }

    /** "Prim" or "Kruskal". Complexity — O(1). */
    public String getAlgorithm() {
        return algorithm;
    }

    /** Edges chosen for the tree. Complexity — O(1). */
    public LinkedList<Edge<T>> getEdges() {
        return edges;
    }

    /** Sum of the weights of the chosen edges. Complexity — O(1). */
    public double getTotalWeight() {
        return totalWeight;
    }

    /**
     * Whether the result connects every vertex of the graph (it has exactly V - 1 edges).
     *
     * <p>Complexity — time and space O(1).
     */
    public boolean spansAllVertices() {
        return vertexCount == 0 || edges.size() == vertexCount - 1;
    }

    /** Whether the edge a–b belongs to the tree. Complexity — O(V). */
    public boolean containsEdge(T a, T b) {
        return edges.find(e -> (e.getFrom().equals(a) && e.getTo().equals(b))
                || (e.getFrom().equals(b) && e.getTo().equals(a))) != null;
    }
}
