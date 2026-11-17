package co.edu.icesi.sgmms.model.structures;

/**
 * Distance matrix produced by Floyd-Warshall. Once computed, the distance between any pair of
 * vertices is answered in O(1): one hash lookup per vertex to get its row and column.
 *
 * @param <T> vertex type
 */
public class AllPairsResult<T> {

    private final HashTable<T, Integer> index;
    private final Object[] vertices;
    private final double[][] distance;
    private final int[][] next;

    AllPairsResult(HashTable<T, Integer> index, Object[] vertices, double[][] distance, int[][] next) {
        this.index = index;
        this.vertices = vertices;
        this.distance = distance;
        this.next = next;
    }

    /**
     * Minimum distance between a and b, or {@link Double#POSITIVE_INFINITY} if they are not
     * connected (or a vertex is unknown).
     *
     * <p>Complexity — time: average O(1); space: O(1).
     */
    public double distance(T a, T b) {
        Integer i = index.get(a);
        Integer j = index.get(b);
        if (i == null || j == null) {
            return Double.POSITIVE_INFINITY;
        }
        return distance[i][j];
    }

    /** Whether a and b are connected. Complexity — average O(1). */
    public boolean isReachable(T a, T b) {
        return distance(a, b) != Double.POSITIVE_INFINITY;
    }

    /**
     * Rebuilds the minimum path from a to b with the successor matrix; empty if unreachable.
     *
     * <p>Complexity — time: O(L) for a path of L vertices; space: O(L).
     */
    @SuppressWarnings("unchecked")
    public LinkedList<T> path(T a, T b) {
        LinkedList<T> path = new LinkedList<>();
        if (!isReachable(a, b)) {
            return path;
        }
        int i = index.get(a);
        int j = index.get(b);
        path.addLast((T) vertices[i]);
        while (i != j) {
            i = next[i][j];
            path.addLast((T) vertices[i]);
        }
        return path;
    }

    /** Number of vertices covered by the matrix. Complexity — O(1). */
    public int vertexCount() {
        return vertices.length;
    }
}
