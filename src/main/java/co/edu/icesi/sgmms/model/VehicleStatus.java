package co.edu.icesi.sgmms.model;

/** State of an emergency vehicle. Only available vehicles can be assigned. */
public enum VehicleStatus {
    AVAILABLE("Disponible"),
    EN_ROUTE("En ruta"),
    ATTENDING("Atendiendo"),
    OUT_OF_SERVICE("Fuera de servicio");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
