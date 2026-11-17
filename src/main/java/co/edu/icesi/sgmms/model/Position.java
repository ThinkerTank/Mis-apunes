package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * Immutable cell coordinate on the city grid. Column {@code x} grows to the right and row
 * {@code y} grows downwards, as on screen. Positions are the vertices of the road graph.
 */
public final class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Adjacent position in {@code direction}. Complexity — time and space O(1). */
    public Position step(Direction direction) {
        return new Position(x + direction.getDx(), y + direction.getDy());
    }

    /** Manhattan distance to {@code other}. Complexity — time and space O(1). */
    public int manhattan(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    /** Whether {@code other} is one of the four orthogonal neighbors. Complexity — O(1). */
    public boolean isAdjacentTo(Position other) {
        return manhattan(other) == 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position)) {
            return false;
        }
        Position position = (Position) other;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
