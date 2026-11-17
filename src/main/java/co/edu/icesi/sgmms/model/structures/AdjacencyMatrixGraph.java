package co.edu.icesi.sgmms.model.structures;

/**
 * Graph stored as a V×V matrix of weights, where {@link Double#POSITIVE_INFINITY} means "no edge".
 *
 * <p>Space is Θ(V²) regardless of the number of edges, but checking or reading an edge is O(1).
 * Floyd-Warshall already works on a V×V matrix, so this representation is the natural input for
 * it; for a sparse grid such as the city map most cells of the matrix are empty.
 *
 * @param <T> vertex type
 */
public class AdjacencyMatrixGraph<T> extends AbstractGraph<T> {

    private static final long serialVersionUID = 1L;

    private double[][] matrix;

    public AdjacencyMatrixGraph() {
        matrix = emptyMatrix(initialCapacity());
    }

    /** Copies the old matrix into a bigger one. Complexity — time O(V²), space O(V²). */
    @Override
    protected void resizeStorage(int newCapacity) {
        double[][] bigger = emptyMatrix(newCapacity);
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], 0, bigger[i], 0, matrix.length);
        }
        matrix = bigger;
    }

    /** Writes both symmetric cells. Complexity — time O(1), space O(1). */
    @Override
    protected void addEdgeInternal(int u, int v, double weight) {
        matrix[u][v] = weight;
        matrix[v][u] = weight;
    }

    @Override
    protected boolean removeEdgeInternal(int u, int v) {
        if (matrix[u][v] == Double.POSITIVE_INFINITY) {
            return false;
        }
        matrix[u][v] = Double.POSITIVE_INFINITY;
        matrix[v][u] = Double.POSITIVE_INFINITY;
        return true;
    }

    @Override
    protected void setWeightInternal(int u, int v, double weight) {
        addEdgeInternal(u, v, weight);
    }

    /** Reads one cell. Complexity — time O(1), space O(1). */
    @Override
    protected double weightInternal(int u, int v) {
        return matrix[u][v];
    }

    /** Scans the whole row of u. Complexity — time O(V), space O(deg(u)). */
    @Override
    protected LinkedList<Neighbor> neighbors(int u) {
        LinkedList<Neighbor> result = new LinkedList<>();
        for (int v = 0; v < vertexCount(); v++) {
            if (matrix[u][v] != Double.POSITIVE_INFINITY) {
                result.addLast(new Neighbor(v, matrix[u][v]));
            }
        }
        return result;
    }

    private static double[][] emptyMatrix(int capacity) {
        double[][] empty = new double[capacity][capacity];
        for (int i = 0; i < capacity; i++) {
            for (int j = 0; j < capacity; j++) {
                empty[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        return empty;
    }
}
