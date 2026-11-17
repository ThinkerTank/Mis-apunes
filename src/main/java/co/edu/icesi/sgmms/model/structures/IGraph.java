package co.edu.icesi.sgmms.model.structures;

/**
 * TAD Graph: undirected graph with non-negative edge weights.
 *
 * <p>Both representations of the project ({@link AdjacencyListGraph} and
 * {@link AdjacencyMatrixGraph}) sit behind this interface, so the city map and the algorithms do
 * not depend on which one is used.
 *
 * @param <T> vertex type; must implement {@code equals} and {@code hashCode} consistently
 */
public interface IGraph<T> {

    /** Adds a vertex. @return {@code false} if it already existed. */
    boolean addVertex(T vertex);

    /**
     * Adds an undirected edge, or replaces its weight if it already exists.
     *
     * @throws IllegalArgumentException if the weight is negative or not finite, or a == b
     */
    void addEdge(T a, T b, double weight);

    /** Removes the edge between a and b. @return {@code false} if there was no such edge. */
    boolean removeEdge(T a, T b);

    /** Changes the weight of an existing edge (used when congestion changes). */
    void updateWeight(T a, T b, double weight);

    /** Vertices connected to {@code vertex} by an edge. */
    LinkedList<T> getAdjacent(T vertex);

    /** Weight of the edge a–b, or {@link Double#POSITIVE_INFINITY} if they are not adjacent. */
    double getWeight(T a, T b);

    /** Whether there is an edge between a and b. */
    boolean hasEdge(T a, T b);

    /** Whether the vertex belongs to the graph. */
    boolean containsVertex(T vertex);

    /** Every vertex, in insertion order. */
    LinkedList<T> vertices();

    /** Every edge, listed once. */
    LinkedList<Edge<T>> edges();

    int vertexCount();

    int edgeCount();

    /** Breadth-first search from {@code start}: reachability and distance in number of edges. */
    BfsResult<T> bfs(T start);

    /** Depth-first search over the whole graph: connected components and cycle detection. */
    DfsResult<T> dfs();

    /** Single-source shortest paths with Dijkstra (min-heap on the project's PriorityQueue). */
    ShortestPathResult<T> dijkstra(T source);

    /** All-pairs shortest distances with Floyd-Warshall. */
    AllPairsResult<T> floydWarshall();

    /** Minimum spanning tree of the component of {@code start} with Prim (min-heap). */
    MstResult<T> primMST(T start);

    /** Minimum spanning forest with Kruskal (own Union-Find). */
    MstResult<T> kruskalMST();
}
