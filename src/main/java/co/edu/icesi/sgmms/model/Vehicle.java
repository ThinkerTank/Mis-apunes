package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;

/**
 * Emergency vehicle (patrol, ambulance or fire truck).
 *
 * <p>While en route it follows the path computed by Dijkstra. Each simulated minute it gains one
 * unit of travel capacity, and entering the next cell costs the current weight of that road
 * segment; a congested segment therefore takes longer to cross, so the route cost is also the
 * travel time in minutes.
 */
public class Vehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final VehicleType type;
    private Position position;
    private VehicleStatus status = VehicleStatus.AVAILABLE;
    private String assignedIncidentId;
    private final LinkedList<Position> route = new LinkedList<>();
    private double travelBudget;

    public Vehicle(String id, VehicleType type, Position position) {
        this.id = id;
        this.type = type;
        this.position = position;
    }

    /** Whether the vehicle can be assigned right now. Complexity — O(1). */
    public boolean isAvailable() {
        return status == VehicleStatus.AVAILABLE;
    }

    /** Whether its type is compatible with the incident type. Complexity — O(1). */
    public boolean canAttend(IncidentType incidentType) {
        return type.canAttend(incidentType);
    }

    /**
     * Sends the vehicle to an incident along {@code path} (the first position is the current one).
     *
     * <p>Complexity — time: O(L) to copy a path of L cells; space: O(L).
     */
    public void dispatch(String incidentId, LinkedList<Position> path) {
        assignedIncidentId = incidentId;
        status = VehicleStatus.EN_ROUTE;
        route.clear();
        boolean first = true;
        for (Position step : path) {
            if (first) {
                first = false;
                continue;
            }
            route.addLast(step);
        }
        travelBudget = 0;
    }

    /** Next cell of the route, or {@code null} if it already arrived. Complexity — O(1). */
    public Position nextStep() {
        return route.isEmpty() ? null : route.getFirst();
    }

    /** Moves one cell along the route. Complexity — O(1). */
    public void advance() {
        position = route.removeFirst();
    }

    /** Remaining cells to the incident. Complexity — O(1). */
    public int remainingSteps() {
        return route.size();
    }

    /** Copy of the remaining route, for drawing it on the map. Complexity — O(L). */
    public LinkedList<Position> remainingRoute() {
        return route.copy();
    }

    /** Arrives at the incident and starts attending it. Complexity — O(1). */
    public void startAttending() {
        route.clear();
        status = VehicleStatus.ATTENDING;
    }

    /** Frees the vehicle after the incident is resolved; it stays where it is. Complexity — O(1). */
    public void release() {
        route.clear();
        assignedIncidentId = null;
        status = VehicleStatus.AVAILABLE;
        travelBudget = 0;
    }

    /** Takes the vehicle out of service or puts it back (only when it is not busy). */
    public void setOutOfService(boolean outOfService) {
        if (outOfService && status == VehicleStatus.AVAILABLE) {
            status = VehicleStatus.OUT_OF_SERVICE;
        } else if (!outOfService && status == VehicleStatus.OUT_OF_SERVICE) {
            status = VehicleStatus.AVAILABLE;
        }
    }

    public String getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    /** Places the vehicle somewhere else (used when loading a configuration). */
    public void setPosition(Position position) {
        this.position = position;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    /** ID of the incident being attended, or {@code null}. */
    public String getAssignedIncidentId() {
        return assignedIncidentId;
    }

    public double getTravelBudget() {
        return travelBudget;
    }

    public void addTravelBudget(double amount) {
        travelBudget += amount;
    }

    public void spendTravelBudget(double amount) {
        travelBudget -= amount;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vehicle && ((Vehicle) other).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + type.getLabel() + " " + status.getLabel() + " " + position;
    }
}
