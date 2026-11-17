package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/** An accident detected in one minute: where, why, and which vehicles were involved. */
public final class AccidentReport {

    private final AccidentCriterion criterion;
    private final Position position;
    private final LinkedList<String> involved;

    public AccidentReport(AccidentCriterion criterion, Position position, LinkedList<String> involved) {
        this.criterion = criterion;
        this.position = position;
        this.involved = involved;
    }

    /** Complexity — time O(1), space O(1). */
    public AccidentCriterion getCriterion() {
        return criterion;
    }

    public Position getPosition() {
        return position;
    }

    /** IDs of the vehicles involved. */
    public LinkedList<String> getInvolved() {
        return involved;
    }

    /**
     * Description stored in the incident.
     *
     * <p>Complexity — time O(k) to print the k involved vehicles; space O(k).
     */
    public String description() {
        return criterion.getDescription() + " (" + involved + ")";
    }
}
