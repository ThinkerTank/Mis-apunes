package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * What the operator decided when assigning a vehicle, and how good that decision was compared
 * with the alternatives available at that moment. It feeds the routing part of the score (RF35).
 */
public class AssignmentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String incidentId;
    private final String vehicleId;
    private final long minute;
    private final double chosenRouteCost;
    private final double bestRouteCost;
    private final double chosenStructuralDistance;
    private final double bestStructuralDistance;

    /**
     * @param chosenRouteCost          Dijkstra cost (current traffic) of the chosen vehicle
     * @param bestRouteCost            lowest Dijkstra cost among compatible available vehicles
     * @param chosenStructuralDistance Floyd-Warshall distance (no congestion) of the chosen vehicle
     * @param bestStructuralDistance   lowest Floyd-Warshall distance among those vehicles
     */
    public AssignmentRecord(String incidentId, String vehicleId, long minute, double chosenRouteCost,
                            double bestRouteCost, double chosenStructuralDistance, double bestStructuralDistance) {
        this.incidentId = incidentId;
        this.vehicleId = vehicleId;
        this.minute = minute;
        this.chosenRouteCost = chosenRouteCost;
        this.bestRouteCost = bestRouteCost;
        this.chosenStructuralDistance = chosenStructuralDistance;
        this.bestStructuralDistance = bestStructuralDistance;
    }

    /** Whether the chosen vehicle had the cheapest Dijkstra route. Complexity — O(1). */
    public boolean choseCheapestRoute() {
        return chosenRouteCost <= bestRouteCost + 1e-9;
    }

    /**
     * Whether the chosen vehicle was structurally much farther than the best one: more than
     * {@code factor} times the optimal Floyd-Warshall distance. Complexity — O(1).
     */
    public boolean exceedsOptimum(double factor) {
        return chosenStructuralDistance > factor * Math.max(bestStructuralDistance, 1e-9);
    }

    /** Estimated travel time in simulated minutes (one unit of route cost per minute). */
    public long estimatedMinutes() {
        return (long) Math.ceil(chosenRouteCost);
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public long getMinute() {
        return minute;
    }

    public double getChosenRouteCost() {
        return chosenRouteCost;
    }

    public double getBestRouteCost() {
        return bestRouteCost;
    }

    public double getChosenStructuralDistance() {
        return chosenStructuralDistance;
    }

    public double getBestStructuralDistance() {
        return bestStructuralDistance;
    }
}
