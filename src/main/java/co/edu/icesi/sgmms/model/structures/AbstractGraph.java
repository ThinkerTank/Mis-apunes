package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.VertexNotFoundException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * Shared part of both graph representations: vertex indexing and the six algorithms.
 *
 * <p>Each vertex receives a consecutive integer index when it is added (kept in a
 * {@link HashTable}, so translating a vertex to its index costs O(1) on average). Subclasses only
 * decide how edges are stored and expose them through {@link #neighbors(int)}. Because of that,
 * every algorithm is written once, and its cost depends on the representation only through
 * {@code neighbors}: O(deg(u)) with adjacency lists, O(V) with the adjacency matrix. That is why
 * BFS and DFS are O(V + E) on lists but O(V²) on the matrix.
 *
 * @param <T> vertex type
 */
public abstract class AbstractGraph<T> implements IGraph<T>, Serializable {

    private static final long serialVersionUID = 1L;

    /** An adjacent vertex (by index) and the weight of the edge that leads to it. */
    protected static final class Neighbor implements Serializable {
        private static final long serialVersionUID = 1L;
        protected final int vertex;
        protected double weight;

        protected Neighbor(int vertex, double weight) {
            this.vertex = vertex;
            this.weight = weight;
        }
    }

    private static final int INITIAL_CAPACITY = 16;

    private final HashTable<T, Integer> indexOf = new HashTable<>();
    private Object[] vertices = new Object[INITIAL_CAPACITY];
    private int vertexCount;
    private int edgeCount;

    protected AbstractGraph() {
        // subclasses allocate their storage for INITIAL_CAPACITY vertices
    }

    /** Initial number of vertex slots that subclasses must allocate. */
    protected static int initialCapacity() {
        return INITIAL_CAPACITY;
    }

    // ---------------------------------------------------------------- storage contract

    /** Grows the edge storage so it can hold {@code newCapacity} vertices. */
    protected abstract void resizeStorage(int newCapacity);

    /** Stores a new undirected edge between two vertex indexes. */
    protected abstract void addEdgeInternal(int u, int v, double weight);

    /** Removes the edge between two vertex indexes; returns whether it existed. */
    protected abstract boolean removeEdgeInternal(int u, int v);

    /** Changes the weight of an existing edge. */
    protected abstract void setWeightInternal(int u, int v, double weight);

    /** Weight of the edge u–v or {@link Double#POSITIVE_INFINITY}. */
    protected abstract double weightInternal(int u, int v);

    /** Adjacent vertices of {@code u}; callers must not modify the returned list. */
    protected abstract LinkedList<Neighbor> neighbors(int u);

    // ---------------------------------------------------------------- basic operations

    /**
     * Adds a vertex.
     *
     * <p>Complexity — time: average O(1) with lists; O(1) amortized with lists but O(V²) when the
     * matrix has to grow (amortized O(V)); space: O(1) amortized.
     */
    @Override
    public boolean addVertex(T vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("Vertices cannot be null");
        }
        if (indexOf.containsKey(vertex)) {
            return false;
        }
        if (vertexCount == vertices.length) {
            int newCapacity = vertices.length * 2;
            Object[] bigger = new Object[newCapacity];
            System.arraycopy(vertices, 0, bigger, 0, vertexCount);
            vertices = bigger;
            resizeStorage(newCapacity);
        }
        vertices[vertexCount] = vertex;
        indexOf.put(vertex, vertexCount);
        vertexCount++;
        return true;
    }

    /**
     * Adds an undirected edge or replaces the weight of an existing one.
     *
     * <p>Complexity — time: O(deg) with lists (a duplicate check walks the list), O(1) with the
     * matrix; space: O(1).
     */
    @Override
    public void addEdge(T a, T b, double weight) {
        validateWeight(weight);
        int u = index(a);
        int v = index(b);
        if (u == v) {
            throw new IllegalArgumentException("Self loops are not allowed: " + a);
        }
        if (weightInternal(u, v) != Double.POSITIVE_INFINITY) {
            setWeightInternal(u, v, weight);
        } else {
            addEdgeInternal(u, v, weight);
            edgeCount++;
        }
    }

    /** Removes an edge. Complexity — time: O(deg) with lists, O(1) with the matrix. */
    @Override
    public boolean removeEdge(T a, T b) {
        int u = index(a);
        int v = index(b);
        boolean removed = removeEdgeInternal(u, v);
        if (removed) {
            edgeCount--;
        }
        return removed;
    }

    @Override
    public void updateWeight(T a, T b, double weight) {
        validateWeight(weight);
        int u = index(a);
        int v = index(b);
        if (weightInternal(u, v) == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("There is no edge between " + a + " and " + b);
        }
        setWeightInternal(u, v, weight);
    }

    /** Adjacent vertices. Complexity — time: O(deg) with lists, O(V) with the matrix. */
    @Override
    public LinkedList<T> getAdjacent(T vertex) {
        LinkedList<T> result = new LinkedList<>();
        for (Neighbor neighbor : neighbors(index(vertex))) {
            result.addLast(vertexAt(neighbor.vertex));
        }
        return result;
    }

    /** Edge weight. Complexity — time: O(deg) with lists, O(1) with the matrix. */
    @Override
    public double getWeight(T a, T b) {
        return weightInternal(index(a), index(b));
    }

    @Override
    public boolean hasEdge(T a, T b) {
        return getWeight(a, b) != Double.POSITIVE_INFINITY;
    }

    @Override
    public boolean containsVertex(T vertex) {
        return vertex != null && indexOf.containsKey(vertex);
    }

    @Override
    public LinkedList<T> vertices() {
        LinkedList<T> result = new LinkedList<>();
        for (int i = 0; i < vertexCount; i++) {
            result.addLast(vertexAt(i));
        }
        return result;
    }

    /** Every edge once. Complexity — time: O(V + E) with lists, O(V²) with the matrix. */
    @Override
    public LinkedList<Edge<T>> edges() {
        LinkedList<Edge<T>> result = new LinkedList<>();
        for (int u = 0; u < vertexCount; u++) {
            for (Neighbor neighbor : neighbors(u)) {
                if (neighbor.vertex > u) {
                    result.addLast(new Edge<>(vertexAt(u), vertexAt(neighbor.vertex), neighbor.weight));
                }
            }
        }
        return result;
    }

    @Override
    public int vertexCount() {
        return vertexCount;
    }

    @Override
    public int edgeCount() {
        return edgeCount;
    }

    // ---------------------------------------------------------------- algorithms

    /**
     * Breadth-first search with the project's {@link Queue}.
     *
     * <p>Complexity — time: O(V + E) with lists, O(V²) with the matrix (every vertex is enqueued
     * once and its neighbors are scanned once); space: O(V) for the queue and the visited marks.
     */
    @Override
    public BfsResult<T> bfs(T start) {
        int s = index(start);
        int[] hops = new int[vertexCount];
        int[] parent = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            hops[i] = -1;
            parent[i] = -1;
        }
        LinkedList<T> order = new LinkedList<>();
        Queue<Integer> queue = new Queue<>();
        hops[s] = 0;
        queue.enqueue(s);
        while (!queue.isEmpty()) {
            int u = queue.dequeue();
            order.addLast(vertexAt(u));
            for (Neighbor neighbor : neighbors(u)) {
                int v = neighbor.vertex;
                if (hops[v] == -1) {
                    hops[v] = hops[u] + 1;
                    parent[v] = u;
                    queue.enqueue(v);
                }
            }
        }
        HashTable<T, Integer> hopTable = new HashTable<>();
        HashTable<T, T> parentTable = new HashTable<>();
        LinkedList<T> unreachable = new LinkedList<>();
        for (int i = 0; i < vertexCount; i++) {
            if (hops[i] >= 0) {
                hopTable.put(vertexAt(i), hops[i]);
                if (parent[i] >= 0) {
                    parentTable.put(vertexAt(i), vertexAt(parent[i]));
                }
            } else {
                unreachable.addLast(vertexAt(i));
            }
        }
        return new BfsResult<>(start, hopTable, parentTable, order, unreachable);
    }

    /**
     * Iterative depth-first search over every vertex, with an explicit {@link Stack} of frames so
     * large maps cannot overflow the call stack. A neighbor that is still on the current path
     * (gray) and is not the parent closes a cycle; both endpoints are reported.
     *
     * <p>Complexity — time: O(V + E) with lists, O(V²) with the matrix; space: O(V).
     */
    @Override
    public DfsResult<T> dfs() {
        final int white = 0;
        final int gray = 1;
        final int black = 2;
        int[] color = new int[vertexCount];
        int[] parent = new int[vertexCount];
        boolean[] closesCycle = new boolean[vertexCount];
        LinkedList<LinkedList<T>> components = new LinkedList<>();
        HashTable<T, Integer> componentOf = new HashTable<>();
        LinkedList<T> discovery = new LinkedList<>();

        for (int s = 0; s < vertexCount; s++) {
            if (color[s] != white) {
                continue;
            }
            LinkedList<T> component = new LinkedList<>();
            int componentId = components.size();
            Stack<int[]> path = new Stack<>();
            Stack<Iterator<Neighbor>> iterators = new Stack<>();
            color[s] = gray;
            parent[s] = -1;
            discover(s, component, componentOf, componentId, discovery);
            path.push(new int[] {s});
            iterators.push(neighbors(s).iterator());
            while (!path.isEmpty()) {
                int u = path.peek()[0];
                Iterator<Neighbor> iterator = iterators.peek();
                if (iterator.hasNext()) {
                    int v = iterator.next().vertex;
                    if (color[v] == white) {
                        color[v] = gray;
                        parent[v] = u;
                        discover(v, component, componentOf, componentId, discovery);
                        path.push(new int[] {v});
                        iterators.push(neighbors(v).iterator());
                    } else if (color[v] == gray && v != parent[u]) {
                        closesCycle[u] = true;
                        closesCycle[v] = true;
                    }
                } else {
                    color[u] = black;
                    path.pop();
                    iterators.pop();
                }
            }
            components.addLast(component);
        }
        LinkedList<T> cycleVertices = new LinkedList<>();
        for (int i = 0; i < vertexCount; i++) {
            if (closesCycle[i]) {
                cycleVertices.addLast(vertexAt(i));
            }
        }
        return new DfsResult<>(components, componentOf, cycleVertices, discovery);
    }

    /**
     * Dijkstra with the project's {@link PriorityQueue} used as a min-heap of tentative
     * distances. Instead of decrease-key, an improved distance is inserted again and stale
     * entries are skipped when extracted (lazy deletion).
     *
     * <p>Complexity — time: O((V + E) log V) with lists (each relaxation may insert into the
     * heap); O(V² log V) with the matrix because of the O(V) neighbor scan; space: O(V + E) for
     * the heap entries.
     */
    @Override
    public ShortestPathResult<T> dijkstra(T source) {
        int s = index(source);
        double[] dist = new double[vertexCount];
        int[] previous = new int[vertexCount];
        boolean[] settled = new boolean[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
            previous[i] = -1;
        }
        dist[s] = 0;
        PriorityQueue<double[]> heap = new PriorityQueue<>((a, b) -> Double.compare(b[1], a[1]));
        heap.insert(new double[] {s, 0});
        while (!heap.isEmpty()) {
            double[] entry = heap.extractMax();
            int u = (int) entry[0];
            if (settled[u]) {
                continue;
            }
            settled[u] = true;
            for (Neighbor neighbor : neighbors(u)) {
                int v = neighbor.vertex;
                double candidate = dist[u] + neighbor.weight;
                if (candidate < dist[v]) {
                    dist[v] = candidate;
                    previous[v] = u;
                    heap.insert(new double[] {v, candidate});
                }
            }
        }
        HashTable<T, Double> distTable = new HashTable<>();
        HashTable<T, T> previousTable = new HashTable<>();
        for (int i = 0; i < vertexCount; i++) {
            if (dist[i] != Double.POSITIVE_INFINITY) {
                distTable.put(vertexAt(i), dist[i]);
                if (previous[i] >= 0) {
                    previousTable.put(vertexAt(i), vertexAt(previous[i]));
                }
            }
        }
        return new ShortestPathResult<>(source, distTable, previousTable);
    }

    /**
     * Floyd-Warshall over a V×V distance matrix plus a successor matrix to rebuild paths.
     *
     * <p>Complexity — time: Θ(V³) in every case (three nested loops over the vertices);
     * space: Θ(V²) for the two matrices.
     */
    @Override
    public AllPairsResult<T> floydWarshall() {
        int n = vertexCount;
        double[][] dist = new double[n][n];
        int[][] next = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = (i == j) ? 0 : Double.POSITIVE_INFINITY;
                next[i][j] = (i == j) ? i : -1;
            }
            for (Neighbor neighbor : neighbors(i)) {
                dist[i][neighbor.vertex] = neighbor.weight;
                next[i][neighbor.vertex] = neighbor.vertex;
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (dist[i][k] == Double.POSITIVE_INFINITY) {
                    continue;
                }
                for (int j = 0; j < n; j++) {
                    double through = dist[i][k] + dist[k][j];
                    if (through < dist[i][j]) {
                        dist[i][j] = through;
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
        HashTable<T, Integer> indexCopy = new HashTable<>(n * 2);
        Object[] verticesCopy = new Object[n];
        for (int i = 0; i < n; i++) {
            verticesCopy[i] = vertices[i];
            indexCopy.put(vertexAt(i), i);
        }
        return new AllPairsResult<>(indexCopy, verticesCopy, dist, next);
    }

    /**
     * Prim from {@code start}, reusing the project's {@link PriorityQueue} as a min-heap of
     * candidate edges (lazy version: edges into vertices already in the tree are discarded when
     * extracted). Only the component of {@code start} is spanned.
     *
     * <p>Complexity — time: O(E log V) with lists (O(E log E) = O(E log V) since E ≤ V²), plus
     * O(V²) neighbor scanning with the matrix; space: O(E).
     */
    @Override
    public MstResult<T> primMST(T start) {
        int s = index(start);
        boolean[] inTree = new boolean[vertexCount];
        PriorityQueue<double[]> heap = new PriorityQueue<>(AbstractGraph::lighterEdgeFirst);
        LinkedList<Edge<T>> treeEdges = new LinkedList<>();
        double total = 0;
        inTree[s] = true;
        pushEdges(s, inTree, heap);
        while (!heap.isEmpty()) {
            double[] edge = heap.extractMax();
            int v = (int) edge[1];
            if (inTree[v]) {
                continue;
            }
            inTree[v] = true;
            treeEdges.addLast(new Edge<>(vertexAt((int) edge[0]), vertexAt(v), edge[2]));
            total += edge[2];
            pushEdges(v, inTree, heap);
        }
        return new MstResult<>("Prim", treeEdges, total, vertexCount);
    }

    /**
     * Kruskal: sorts every edge by weight (merge sort on the project's {@link LinkedList}) and
     * adds it unless {@link UnionFind} reports that its endpoints are already connected.
     *
     * <p>Complexity — time: O(E log E) = O(E log V) for sorting, plus O(E · α(V)) for the
     * Union-Find operations; space: O(V + E).
     */
    @Override
    public MstResult<T> kruskalMST() {
        LinkedList<double[]> all = new LinkedList<>();
        for (int u = 0; u < vertexCount; u++) {
            for (Neighbor neighbor : neighbors(u)) {
                if (neighbor.vertex > u) {
                    all.addLast(new double[] {u, neighbor.vertex, neighbor.weight});
                }
            }
        }
        all.sort((a, b) -> lighterEdgeFirst(b, a));
        UnionFind sets = new UnionFind(vertexCount);
        LinkedList<Edge<T>> treeEdges = new LinkedList<>();
        double total = 0;
        for (double[] edge : all) {
            if (sets.union((int) edge[0], (int) edge[1])) {
                treeEdges.addLast(new Edge<>(vertexAt((int) edge[0]), vertexAt((int) edge[1]), edge[2]));
                total += edge[2];
                if (treeEdges.size() == vertexCount - 1) {
                    break;
                }
            }
        }
        return new MstResult<>("Kruskal", treeEdges, total, vertexCount);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Index of a vertex. Complexity — average O(1).
     *
     * @throws VertexNotFoundException if the vertex is not in the graph
     */
    protected int index(T vertex) {
        Integer i = vertex == null ? null : indexOf.get(vertex);
        if (i == null) {
            throw new VertexNotFoundException(vertex);
        }
        return i;
    }

    @SuppressWarnings("unchecked")
    protected T vertexAt(int i) {
        return (T) vertices[i];
    }

    private void discover(int v, LinkedList<T> component, HashTable<T, Integer> componentOf,
                          int componentId, LinkedList<T> discovery) {
        T vertex = vertexAt(v);
        component.addLast(vertex);
        componentOf.put(vertex, componentId);
        discovery.addLast(vertex);
    }

    private void pushEdges(int u, boolean[] inTree, PriorityQueue<double[]> heap) {
        for (Neighbor neighbor : neighbors(u)) {
            if (!inTree[neighbor.vertex]) {
                heap.insert(new double[] {u, neighbor.vertex, neighbor.weight});
            }
        }
    }

    /** Orders candidate edges {from, to, weight}: lighter first, then by indexes (determinism). */
    private static int lighterEdgeFirst(double[] a, double[] b) {
        int byWeight = Double.compare(b[2], a[2]);
        if (byWeight != 0) {
            return byWeight;
        }
        int byFrom = Double.compare(b[0], a[0]);
        return byFrom != 0 ? byFrom : Double.compare(b[1], a[1]);
    }

    private static void validateWeight(double weight) {
        if (weight < 0 || Double.isNaN(weight) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Edge weights must be finite and non-negative: " + weight);
        }
    }
}
