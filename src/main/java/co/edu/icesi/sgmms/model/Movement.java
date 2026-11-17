package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/** One valid step of the operator on the map; kept on a stack so it can be undone. */
public class Movement implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Position from;
    private final Position to;
    private final Direction direction;
    private final long minute;

    public Movement(Position from, Position to, Direction direction, long minute) {
        this.from = from;
        this.to = to;
        this.direction = direction;
        this.minute = minute;
    }

    /** Position before the step; undo restores it. */
    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getMinute() {
        return minute;
    }

    @Override
    public String toString() {
        return direction.getLabel() + " " + from + " → " + to;
    }
}
