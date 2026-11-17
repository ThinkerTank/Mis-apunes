package co.edu.icesi.sgmms.controller;

/** The three connected scenarios of the prototype, with their FXML view. */
public enum Screen {
    MONITORING("MonitoringCenterView.fxml"),
    MAP("TrafficMapView.fxml"),
    PANEL("IncidentPanelView.fxml");

    private final String fxml;

    Screen(String fxml) {
        this.fxml = fxml;
    }

    /** Classpath location of the view. */
    public String resource() {
        return "/co/edu/icesi/sgmms/view/" + fxml;
    }
}
