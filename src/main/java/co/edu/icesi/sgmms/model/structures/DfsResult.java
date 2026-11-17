package co.edu.icesi.sgmms.model.structures;

/**
 * Result of a depth-first search over the whole graph: its connected components and the
 * vertices where cycles close (endpoints of back edges).
 *
 * @param <T> vertex type
 */
public class DfsResult<T> {

    private final LinkedList<LinkedList<T>> components;
    private final HashTable<T, Integer> componentOf;
    private final LinkedList<T> cycleVertices;
    private final LinkedList<T> discoveryOrder;

    DfsResult(LinkedList<LinkedList<T>> components, HashTable<T, Integer> componentOf,
              LinkedList<T> cycleVertices, LinkedList<T> discoveryOrder) {
        this.components = components;
        this.componentOf = componentOf;
        this.cycleVertices = cycleVertices;
        this.discoveryOrder = discoveryOrder;
    }

    /** Connected components; each one lists its vertices in discovery order. Complexity — O(1). */
    public LinkedList<LinkedList<T>> components() {
        return components;
    }

    /** Number of connected components. Complexity — O(1). */
    public int componentCount() {
        return components.size();
    }

    /**
     * Index of the component that contains {@code vertex}, or -1 if it is not in the graph.
     *
     * <p>Complexity — time: average O(1); space: O(1).
     */
    public int componentOf(T vertex) {
        Integer id = componentOf.get(vertex);
        return id == null ? -1 : id;
    }

    /** Whether two vertices are connected. Complexity — average O(1). */
    public boolean sameComponent(T a, T b) {
        int first = componentOf(a);
        return first >= 0 && first == componentOf(b);
    }

    /** Whether the graph contains at least one cycle. Complexity — O(1). */
    public boolean hasCycle() {
        return !cycleVertices.isEmpty();
    }

    /** Vertices that close a cycle (each listed once). Complexity — O(1). */
    public LinkedList<T> cycleVertices() {
        return cycleVertices;
    }

    /** Vertices in the order DFS discovered them. Complexity — O(1). */
    public LinkedList<T> discoveryOrder() {
        return discoveryOrder;
    }
}
