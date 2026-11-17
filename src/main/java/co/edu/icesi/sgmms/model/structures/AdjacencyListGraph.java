package co.edu.icesi.sgmms.model.structures;

/**
 * Graph stored as adjacency lists: one project {@link LinkedList} of neighbors per vertex.
 *
 * <p>Space is Θ(V + E). It is the representation used for analysis on the city map, which is a
 * sparse grid (each intersection has at most four neighbors, so E ≤ 2V).
 *
 * @param <T> vertex type
 */
public class AdjacencyListGraph<T> extends AbstractGraph<T> {

    private static final long serialVersionUID = 1L;

    private LinkedList<Neighbor>[] adjacency;

    public AdjacencyListGraph() {
        adjacency = newLists(initialCapacity());
    }

    @Override
    protected void resizeStorage(int newCapacity) {
        LinkedList<Neighbor>[] bigger = newLists(newCapacity);
        System.arraycopy(adjacency, 0, bigger, 0, adjacency.length);
        adjacency = bigger;
    }

    /** Appends one neighbor on each side. Complexity — time O(1), space O(1). */
    @Override
    protected void addEdgeInternal(int u, int v, double weight) {
        adjacency[u].addLast(new Neighbor(v, weight));
        adjacency[v].addLast(new Neighbor(u, weight));
    }

    /** Removes the neighbor on each side. Complexity — time O(deg(u) + deg(v)), space O(1). */
    @Override
    protected boolean removeEdgeInternal(int u, int v) {
        boolean removed = adjacency[u].removeIf(n -> n.vertex == v);
        adjacency[v].removeIf(n -> n.vertex == u);
        return removed;
    }

    @Override
    protected void setWeightInternal(int u, int v, double weight) {
        adjacency[u].find(n -> n.vertex == v).weight = weight;
        adjacency[v].find(n -> n.vertex == u).weight = weight;
    }

    /** Looks the neighbor up in the list of u. Complexity — time O(deg(u)), space O(1). */
    @Override
    protected double weightInternal(int u, int v) {
        Neighbor neighbor = adjacency[u].find(n -> n.vertex == v);
        return neighbor == null ? Double.POSITIVE_INFINITY : neighbor.weight;
    }

    @Override
    protected LinkedList<Neighbor> neighbors(int u) {
        return adjacency[u];
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<Neighbor>[] newLists(int capacity) {
        LinkedList<Neighbor>[] lists = (LinkedList<Neighbor>[]) new LinkedList[capacity];
        for (int i = 0; i < capacity; i++) {
            lists[i] = new LinkedList<>();
        }
        return lists;
    }
}
