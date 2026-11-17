package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * Administrative action of the operator (assigning a vehicle, resolving an incident, closing or
 * reopening a road). Actions are pushed onto a stack so the panel can show the last one (RF22).
 */
public class OperatorAction implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Kinds of administrative action. */
    public enum Type {
        ASSIGN_VEHICLE("Asignar vehículo"),
        RESOLVE_INCIDENT("Resolver incidente"),
        BLOCK_ROAD("Bloquear vía"),
        UNBLOCK_ROAD("Desbloquear vía");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Type type;
    private final String detail;
    private final long minute;

    public OperatorAction(Type type, String detail, long minute) {
        this.type = type;
        this.detail = detail;
        this.minute = minute;
    }

    /** Complexity — time O(1), space O(1). */
    public Type getType() {
        return type;
    }

    /** Complexity — time O(1), space O(1). */
    public String getDetail() {
        return detail;
    }

    public long getMinute() {
        return minute;
    }

    @Override
    public String toString() {
        return "[min " + minute + "] " + type.getLabel() + ": " + detail;
    }
}
