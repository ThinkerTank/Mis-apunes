package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.InvalidIncidentStateException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An accident, robbery or fire reported on the map.
 *
 * <p>Severity, generation time and position never change after creation, so the incident keeps
 * its place in the binary search tree and in the priority queue for its whole life. Only the
 * status, the assigned vehicle and the resolution time change.
 */
public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final IncidentType type;
    private final Position position;
    private final Severity severity;
    private final String description;
    private final LocalDateTime createdAt;
    private final long generatedAtMinute;
    private final long sequence;

    private IncidentStatus status = IncidentStatus.PENDING;
    private String assignedVehicleId;
    private long assignedAtMinute = -1;
    private long resolvedAtMinute = -1;

    /**
     * @param generatedAtMinute simulation minute in which the incident appeared
     * @param sequence          global creation counter; breaks ties between incidents created in
     *                          the same minute with the same severity
     */
    public Incident(String id, IncidentType type, Position position, Severity severity,
                    String description, LocalDateTime createdAt, long generatedAtMinute, long sequence) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.severity = severity;
        this.description = description;
        this.createdAt = createdAt;
        this.generatedAtMinute = generatedAtMinute;
        this.sequence = sequence;
    }

    /**
     * Records that a vehicle was assigned and the attention started its way.
     *
     * <p>Complexity — time and space O(1).
     *
     * @throws InvalidIncidentStateException if the incident is not pending
     */
    public void markInProgress(String vehicleId, long minute) {
        if (status == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentStateException("Incident " + id + " is already resolved and cannot receive vehicles");
        }
        if (status == IncidentStatus.IN_PROGRESS) {
            throw new InvalidIncidentStateException("Incident " + id + " is already being attended by " + assignedVehicleId);
        }
        status = IncidentStatus.IN_PROGRESS;
        assignedVehicleId = vehicleId;
        assignedAtMinute = minute;
    }

    /**
     * Records the resolution of the incident.
     *
     * <p>Complexity — time and space O(1).
     *
     * @throws InvalidIncidentStateException if no vehicle is attending it
     */
    public void markResolved(long minute) {
        if (status != IncidentStatus.IN_PROGRESS) {
            throw new InvalidIncidentStateException("Incident " + id + " has no vehicle attending it");
        }
        status = IncidentStatus.RESOLVED;
        resolvedAtMinute = minute;
    }

    /** Whether the incident still needs attention (pending or in progress). Complexity — O(1). */
    public boolean isActive() {
        return status != IncidentStatus.RESOLVED;
    }

    /** Minutes between generation and resolution, or -1 while unresolved. Complexity — O(1). */
    public long attentionMinutes() {
        return resolvedAtMinute < 0 ? -1 : resolvedAtMinute - generatedAtMinute;
    }

    public String getId() {
        return id;
    }

    public IncidentType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Date and time of generation, formatted for the interface. */
    public String getCreatedAtLabel() {
        return createdAt.format(CLOCK);
    }

    public long getGeneratedAtMinute() {
        return generatedAtMinute;
    }

    public long getSequence() {
        return sequence;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    /** ID of the assigned vehicle, or {@code null}. */
    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public long getAssignedAtMinute() {
        return assignedAtMinute;
    }

    /** Simulation minute of the resolution, or -1. */
    public long getResolvedAtMinute() {
        return resolvedAtMinute;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Incident && ((Incident) other).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + type.getLabel() + " " + severity.getLabel() + " " + position;
    }
}
