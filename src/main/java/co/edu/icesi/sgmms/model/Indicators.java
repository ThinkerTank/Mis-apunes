package co.edu.icesi.sgmms.model;

/**
 * Snapshot of the real-time indicators of the monitoring center (RF13). It is immutable, so the
 * interface thread can read it while the simulation threads keep changing the model.
 */
public final class Indicators {

    private final long minute;
    private final int score;
    private final int activeIncidents;
    private final int accidents;
    private final int robberies;
    private final int fires;
    private final int pendingIncidents;
    private final int availableVehicles;
    private final int totalVehicles;
    private final int resolvedIncidents;

    public Indicators(long minute, int score, int activeIncidents, int accidents, int robberies, int fires,
                      int pendingIncidents, int availableVehicles, int totalVehicles, int resolvedIncidents) {
        this.minute = minute;
        this.score = score;
        this.activeIncidents = activeIncidents;
        this.accidents = accidents;
        this.robberies = robberies;
        this.fires = fires;
        this.pendingIncidents = pendingIncidents;
        this.availableVehicles = availableVehicles;
        this.totalVehicles = totalVehicles;
        this.resolvedIncidents = resolvedIncidents;
    }

    public long getMinute() {
        return minute;
    }

    public int getScore() {
        return score;
    }

    public int getActiveIncidents() {
        return activeIncidents;
    }

    public int getAccidents() {
        return accidents;
    }

    public int getRobberies() {
        return robberies;
    }

    public int getFires() {
        return fires;
    }

    public int getPendingIncidents() {
        return pendingIncidents;
    }

    public int getAvailableVehicles() {
        return availableVehicles;
    }

    public int getTotalVehicles() {
        return totalVehicles;
    }

    public int getResolvedIncidents() {
        return resolvedIncidents;
    }
}
