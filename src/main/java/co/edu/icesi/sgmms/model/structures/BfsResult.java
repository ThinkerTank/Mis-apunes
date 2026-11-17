package co.edu.icesi.sgmms.model.structures;

/**
 * Result of a breadth-first search: which vertices are reachable from the start vertex and how
 * many edges (hops) separate each one from it.
 *
 * @param <T> vertex type
 */
public class BfsResult<T> {

    private final T start;
    private final HashTable<T, Integer> hops;
    private final HashTable<T, T> parent;
    private final LinkedList<T> visitOrder;
    private final LinkedList<T> unreachable;

    BfsResult(T start, HashTable<T, Integer> hops, HashTable<T, T> parent,
              LinkedList<T> visitOrder, LinkedList<T> unreachable) {
        this.start = start;
        this.hops = hops;
        this.parent = parent;
        this.visitOrder = visitOrder;
        this.unreachable = unreachable;
    }

    /** Start vertex of the search. Complexity — O(1). */
    public T getStart() {
        return start;
    }

    /** Whether {@code vertex} can be reached from the start. Complexity — average O(1). */
    public boolean isReachable(T vertex) {
        return hops.containsKey(vertex);
    }

    /**
     * Minimum number of edges from the start to {@code vertex}, or -1 if it is unreachable.
     *
     * <p>Complexity — time: average O(1) (hash lookup); space: O(1).
     */
    public int hopsTo(T vertex) {
        Integer value = hops.get(vertex);
        return value == null ? -1 : value;
    }

    /**
     * Path with the fewest edges from the start to {@code vertex}; empty if unreachable.
     *
     * <p>Complexity — time: O(L) for a path of L vertices; space: O(L).
     */
    public LinkedList<T> pathTo(T vertex) {
        LinkedList<T> path = new LinkedList<>();
        if (!isReachable(vertex)) {
            return path;
        }
        for (T current = vertex; current != null; current = parent.get(current)) {
            path.addFirst(current);
        }
        return path;
    }

    /** Vertices in the order they were discovered. Complexity — O(1). */
    public LinkedList<T> visitOrder() {
        return visitOrder;
    }

    /** Vertices that cannot be reached from the start. Complexity — O(1). */
    public LinkedList<T> unreachable() {
        return unreachable;
    }

    /** Number of reachable vertices, including the start. Complexity — O(1). */
    public int reachableCount() {
        return visitOrder.size();
    }

    /** Largest hop distance among reachable vertices. Complexity — O(V). */
    public int maxHops() {
        int max = 0;
        for (Integer value : hops.values()) {
            max = Math.max(max, value);
        }
        return max;
    }
}
