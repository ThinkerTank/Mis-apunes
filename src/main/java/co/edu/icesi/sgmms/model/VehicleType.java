package co.edu.icesi.sgmms.model;

/**
 * Kinds of emergency vehicle and the assignment rules of the statement: a patrol attends
 * robberies and supports accidents, an ambulance attends accidents and a fire truck attends fires.
 */
public enum VehicleType {
    PATROL("Patrulla"),
    AMBULANCE("Ambulancia"),
    FIRE_TRUCK("Camión de bomberos");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Whether a vehicle of this type may be sent to an incident of {@code type}. Complexity — O(1). */
    public boolean canAttend(IncidentType type) {
        switch (this) {
            case PATROL:
                return type == IncidentType.ROBBERY || type == IncidentType.ACCIDENT;
            case AMBULANCE:
                return type == IncidentType.ACCIDENT;
            case FIRE_TRUCK:
                return type == IncidentType.FIRE;
            default:
                return false;
        }
    }
}
