package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.Position;

/** Where a moving vehicle was and where it ended up during one simulated minute. */
public final class CarMove {

    private final String vehicleId;
    private final Position from;
    private final Position to;

    public CarMove(String vehicleId, Position from, Position to) {
        this.vehicleId = vehicleId;
        this.from = from;
        this.to = to;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    /** Complexity — time O(1), space O(1). */
    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    /**
     * Whether the vehicle actually changed cell.
     *
     * <p>Complexity — time O(1), space O(1).
     */
    public boolean moved() {
        return !from.equals(to);
    }
}
