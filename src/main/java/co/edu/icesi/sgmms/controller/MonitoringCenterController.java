package co.edu.icesi.sgmms.controller;

import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.ZoneCoverage;
import co.edu.icesi.sgmms.view.Palette;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Main screen (scenario 1): the general state of the city. It shows the system name, the
 * operator's score, the number of active incidents in total and by type, the available vehicles,
 * the priorities (BST, priority queue and dispatch queue), the road-network analysis (BFS
 * coverage, Floyd-Warshall response times, DFS components and risk points) and the recent
 * activity, plus shortcuts to the other two scenarios.
 */
public class MonitoringCenterController implements ScreenController {

    @FXML private FlowPane cards;
    @FXML private Label mostUrgentActiveLabel;
    @FXML private Label mostUrgentPendingLabel;
    @FXML private Label nextInQueueLabel;
    @FXML private Label pendingCountLabel;
    @FXML private GridPane coverageGrid;
    @FXML private Label networkLabel;
    @FXML private ListView<String> activityList;

    private AppContext context;
    private Label scoreValue;
    private Label activeValue;
    private Label accidentsValue;
    private Label robberiesValue;
    private Label firesValue;
    private Label vehiclesValue;
    private Label resolvedValue;

    /** Complexity — time O(1): builds seven indicator cards; space O(1). */
    @Override
    public void init(AppContext appContext) {
        this.context = appContext;
        scoreValue = card("Puntaje del operador", "#facc15");
        activeValue = card("Incidentes activos", "#0f172a");
        accidentsValue = card("Accidentes activos", Palette.css(Palette.incident(IncidentType.ACCIDENT)));
        robberiesValue = card("Robos activos", Palette.css(Palette.incident(IncidentType.ROBBERY)));
        firesValue = card("Incendios activos", Palette.css(Palette.incident(IncidentType.FIRE)));
        vehiclesValue = card("Vehículos disponibles", "#0f766e");
        resolvedValue = card("Incidentes resueltos", "#15803d");
        activityList.setItems(context.activity());
        activityList.setPlaceholder(new Label("Todavía no hay actividad. Inicie la simulación."));
    }

    @Override
    public void onShow() {
        // nothing to prepare: the screen is fully rebuilt by refresh()
    }

    /** Complexity — time O(n + m + V + E): indicator snapshot O(n + m), BFS coverage and DFS components O(V + E), response time per zone O(1) each from the Floyd-Warshall matrix; space O(V). */
    @Override
    public void refresh() {
        DispatchCenter center = context.center();
        Indicators now = center.indicators();
        scoreValue.setText(String.valueOf(now.getScore()));
        activeValue.setText(String.valueOf(now.getActiveIncidents()));
        accidentsValue.setText(String.valueOf(now.getAccidents()));
        robberiesValue.setText(String.valueOf(now.getRobberies()));
        firesValue.setText(String.valueOf(now.getFires()));
        vehiclesValue.setText(now.getAvailableVehicles() + " / " + now.getTotalVehicles());
        resolvedValue.setText(String.valueOf(now.getResolvedIncidents()));

        mostUrgentActiveLabel.setText(describe(center.mostUrgentActive()));
        mostUrgentPendingLabel.setText(describe(center.mostUrgentPending()));
        nextInQueueLabel.setText(describe(center.nextInDispatchQueue()));
        pendingCountLabel.setText(now.getPendingIncidents() + " incidentes esperan vehículo");

        coverageGrid.getChildren().clear();
        coverageGrid.addRow(0, header("Zona"), header("Celdas"), header("Saltos prom. (BFS)"),
                header("Saltos máx."), header("Tiempo resp. (F-W)"));
        int row = 1;
        for (ZoneCoverage zone : center.coverage()) {
            double minutes = center.responseTime(zone.getZone());
            coverageGrid.addRow(row++, cell(zone.getZone().getLabel()), cell(String.valueOf(zone.getCells())),
                    cell(String.format(Locale.ROOT, "%.1f", zone.getAverageHops())), cell(String.valueOf(zone.getMaxHops())),
                    cell(Double.isNaN(minutes) ? "—" : String.format(Locale.ROOT, "%.1f min", minutes)));
        }
        networkLabel.setText(String.format(Locale.ROOT,
                "Componentes conexas (DFS): %d   ·   Puntos de riesgo: %d   ·   Vías bloqueadas: %d   ·   "
                        + "Floyd-Warshall: %d ms (se recalcula solo al cambiar la topología)",
                center.connectedComponents(), center.riskPoints().size(), center.blockedRoads().size(),
                center.lastFloydWarshallMillis()));
    }

    @FXML
    private void openMap() {
        context.navigate(Screen.MAP);
    }

    @FXML
    private void openPanel() {
        context.navigate(Screen.PANEL);
    }

    private Label card(String caption, String accent) {
        Label value = new Label("0");
        value.getStyleClass().add("card-value");
        Label title = new Label(caption);
        title.getStyleClass().add("card-caption");
        VBox box = new VBox(4, title, value);
        box.getStyleClass().add("indicator-card");
        box.setStyle("-fx-border-color: transparent transparent transparent " + accent + ";");
        cards.getChildren().add(box);
        return value;
    }

    private static String describe(Incident incident) {
        if (incident == null) {
            return "—";
        }
        return incident.getId() + " · " + incident.getType().getLabel() + " · gravedad " + incident.getSeverity().getLabel()
                + " · " + incident.getStatus().getLabel() + " · " + incident.getPosition();
    }

    private static Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("grid-header");
        return label;
    }

    private static Label cell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("grid-cell");
        return label;
    }
}
