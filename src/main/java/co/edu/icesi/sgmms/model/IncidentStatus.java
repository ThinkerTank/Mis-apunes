package co.edu.icesi.sgmms.model;

/** Life cycle of an incident: pending (no vehicle) → in progress (vehicle assigned) → resolved. */
public enum IncidentStatus {
    PENDING("Pendiente"),
    IN_PROGRESS("En proceso"),
    RESOLVED("Resuelto");

    private final String label;

    IncidentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
