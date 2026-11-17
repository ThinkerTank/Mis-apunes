package co.edu.icesi.sgmms.model.structures;

/**
 * Disjoint-set (Union-Find) over the integers {@code 0 .. n-1}, used by Kruskal to know whether
 * an edge would close a cycle.
 *
 * <p>Two optimizations are applied together: <b>union by rank</b> hangs the shorter tree under
 * the taller one, and <b>path compression</b> makes every node visited by {@code find} point
 * directly to the root. With both, a sequence of m operations costs O(m · α(n)), where α is the
 * inverse Ackermann function (at most 4 for any realistic n), i.e. practically constant per
 * operation. Without them, a chain of unions can degrade {@code find} to O(n).
 */
public class UnionFind {

    private final int[] parent;
    private final int[] rank;
    private int components;

    /** Creates n singleton sets. Complexity — time O(n), space O(n). */
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        components = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    /**
     * Returns the representative of the set that contains {@code x}, compressing the path.
     *
     * <p>Complexity — time: best O(1) (x is a root), amortized O(α(n)), worst O(log n) for a
     * single call with union by rank; space: O(1) (iterative).
     */
    public int find(int x) {
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /**
     * Merges the sets of {@code a} and {@code b}.
     *
     * <p>Complexity — time: amortized O(α(n)); worst O(log n); space: O(1).
     *
     * @return {@code false} if they were already in the same set (the edge would close a cycle)
     */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
        components--;
        return true;
    }

    /** Whether a and b belong to the same set. Complexity — amortized O(α(n)). */
    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    /** Number of disjoint sets. Complexity — O(1). */
    public int componentCount() {
        return components;
    }
}
