package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import co.edu.icesi.sgmms.model.exceptions.InvalidMovementException;
import co.edu.icesi.sgmms.model.structures.Stack;
import java.io.Serializable;

/**
 * Position of the operator's cursor on the traffic map and its movement history (RF21).
 *
 * <p>Every valid movement (W, A, S, D or the arrow keys) is pushed onto a {@link Stack}; undo (U
 * key or "Undo" button) pops it and restores the previous position. The operator cannot leave the
 * map or step onto an obstacle.
 */
public class OperatorNavigator implements Serializable {

    private static final long serialVersionUID = 1L;

    private final CityMap map;
    private final Stack<Movement> history = new Stack<>();
    private Position position;

    public OperatorNavigator(CityMap map, Position start) {
        this.map = map;
        this.position = start;
    }

    /**
     * Moves one cell in {@code direction} and records the movement.
     *
     * <p>Complexity — time: O(1) (bounds check, one cell lookup and a push); space: O(1).
     *
     * @throws InvalidMovementException if the target is outside the map or an obstacle
     */
    public Movement move(Direction direction, long minute) {
        Position target = position.step(direction);
        if (!map.isInside(target)) {
            throw new InvalidMovementException("You cannot leave the map: " + target + " is outside the city limits");
        }
        if (!map.canEnter(target)) {
            throw new InvalidMovementException("There is an obstacle at " + target);
        }
        Movement movement = new Movement(position, target, direction, minute);
        history.push(movement);
        position = target;
        return movement;
    }

    /**
     * Undoes the last movement, going back to where it started.
     *
     * <p>Complexity — time: O(1) (one pop); space: O(1).
     *
     * @throws EmptyStructureException if there is no movement to undo
     */
    public Movement undo() {
        if (history.isEmpty()) {
            throw new EmptyStructureException("There are no movements to undo");
        }
        Movement movement = history.pop();
        position = movement.getFrom();
        return movement;
    }

    public Position getPosition() {
        return position;
    }

    /** Number of movements that can still be undone. Complexity — O(1). */
    public int historySize() {
        return history.size();
    }

    /** Last movement without undoing it, or {@code null}. Complexity — O(1). */
    public Movement lastMovement() {
        return history.isEmpty() ? null : history.peek();
    }
}
