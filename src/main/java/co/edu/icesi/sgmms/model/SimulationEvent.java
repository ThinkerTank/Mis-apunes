package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * Something that happened in the simulation. The model enqueues events and the interface consumes
 * them to refresh indicators and the activity log (RF18, RF24).
 */
public class SimulationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Kinds of simulation event. */
    public enum Type {
        INCIDENT_CREATED("Incidente generado"),
        VEHICLE_ASSIGNED("Vehículo asignado"),
        VEHICLE_ARRIVED("Vehículo en el lugar"),
        INCIDENT_RESOLVED("Incidente resuelto"),
        VEHICLE_STATUS_CHANGED("Cambio de estado de vehículo"),
        ROAD_BLOCKED("Vía bloqueada"),
        ROAD_UNBLOCKED("Vía desbloqueada"),
        ISOLATED_INCIDENT("Incidente aislado");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Type type;
    private final String message;
    private final long minute;

    public SimulationEvent(Type type, String message, long minute) {
        this.type = type;
        this.message = message;
        this.minute = minute;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getMinute() {
        return minute;
    }

    @Override
    public String toString() {
        return "[min " + minute + "] " + type.getLabel() + ": " + message;
    }
}
