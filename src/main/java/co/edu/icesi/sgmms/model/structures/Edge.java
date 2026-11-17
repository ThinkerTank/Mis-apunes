package co.edu.icesi.sgmms.model.structures;

import java.io.Serializable;

/**
 * Undirected weighted edge between two vertices.
 *
 * @param <T> vertex type
 */
public class Edge<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final T from;
    private final T to;
    private final double weight;

    public Edge(T from, T to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    /** One endpoint. Complexity — O(1). */
    public T getFrom() {
        return from;
    }

    /** The other endpoint. Complexity — O(1). */
    public T getTo() {
        return to;
    }

    /** Weight (cost) of the edge. Complexity — O(1). */
    public double getWeight() {
        return weight;
    }

    /** Whether {@code vertex} is one of the two endpoints. Complexity — O(1). */
    public boolean touches(T vertex) {
        return from.equals(vertex) || to.equals(vertex);
    }

    @Override
    public String toString() {
        return from + " -(" + weight + ")- " + to;
    }
}
