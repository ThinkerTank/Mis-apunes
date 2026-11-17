package co.edu.icesi.sgmms.model;

/** The three kinds of incident the monitoring center handles. */
public enum IncidentType {
    ACCIDENT("Accidente"),
    ROBBERY("Robo"),
    FIRE("Incendio");

    private final String label;

    IncidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
