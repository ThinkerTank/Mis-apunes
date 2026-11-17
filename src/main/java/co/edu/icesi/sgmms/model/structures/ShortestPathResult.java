package co.edu.icesi.sgmms.model.structures;

/**
 * Result of Dijkstra from one source: minimum distance and path to every vertex.
 *
 * @param <T> vertex type
 */
public class ShortestPathResult<T> {

    private final T source;
    private final HashTable<T, Double> distance;
    private final HashTable<T, T> previous;

    ShortestPathResult(T source, HashTable<T, Double> distance, HashTable<T, T> previous) {
        this.source = source;
        this.distance = distance;
        this.previous = previous;
    }

    /** Source vertex. Complexity — O(1). */
    public T getSource() {
        return source;
    }

    /**
     * Minimum distance from the source, or {@link Double#POSITIVE_INFINITY} if unreachable.
     *
     * <p>Complexity — time: average O(1); space: O(1).
     */
    public double distanceTo(T vertex) {
        Double value = distance.get(vertex);
        return value == null ? Double.POSITIVE_INFINITY : value;
    }

    /** Whether {@code vertex} can be reached from the source. Complexity — average O(1). */
    public boolean isReachable(T vertex) {
        return distance.containsKey(vertex);
    }

    /**
     * Minimum-cost path from the source to {@code vertex}, both included; empty if unreachable.
     *
     * <p>Complexity — time: O(L) for a path of L vertices; space: O(L).
     */
    public LinkedList<T> pathTo(T vertex) {
        LinkedList<T> path = new LinkedList<>();
        if (!isReachable(vertex)) {
            return path;
        }
        for (T current = vertex; current != null; current = previous.get(current)) {
            path.addFirst(current);
        }
        return path;
    }
}
