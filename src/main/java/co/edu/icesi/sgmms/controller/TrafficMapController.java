package co.edu.icesi.sgmms.controller;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.Direction;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Movement;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.VehicleType;
import co.edu.icesi.sgmms.model.ZoneType;
import co.edu.icesi.sgmms.model.exceptions.SgmmsException;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import co.edu.icesi.sgmms.model.structures.MstResult;
import co.edu.icesi.sgmms.view.MapRenderer;
import co.edu.icesi.sgmms.view.MapScene;
import co.edu.icesi.sgmms.view.Palette;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Traffic map (scenario 2): a simplified Palmira with residential and commercial zones, main
 * roads, obstacles, available routes, emergency vehicles, private cars and active incidents.
 *
 * <p>The operator moves the yellow cursor with W/A/S/D or the arrow keys and undoes the last
 * movement with U (movement stack, RF21). Optional layers show the emergency corridors (MST with
 * Prim or Kruskal, RF34), the risk points (DFS, RF31) and the Dijkstra route of every vehicle en
 * route (RF32). In road mode, clicking two adjacent cells blocks or reopens the segment between
 * them (RF31a).
 */
public class TrafficMapController implements ScreenController {

    @FXML private Pane canvasHolder;
    @FXML private Canvas canvas;
    @FXML private Label operatorLabel;
    @FXML private Label movementLabel;
    @FXML private CheckBox corridorsCheck;
    @FXML private ComboBox<String> algorithmBox;
    @FXML private Label corridorsLabel;
    @FXML private CheckBox riskCheck;
    @FXML private CheckBox routesCheck;
    @FXML private CheckBox civiliansCheck;
    @FXML private ToggleButton roadModeButton;
    @FXML private Label roadLabel;
    @FXML private Label selectionLabel;
    @FXML private VBox legendBox;

    private final MapRenderer renderer = new MapRenderer();
    private AppContext context;
    private Position selected;
    private Position pendingBlock;
    private String highlightedVehicle;

    @Override
    public void init(AppContext appContext) {
        this.context = appContext;
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
        algorithmBox.getItems().addAll("Prim", "Kruskal");
        algorithmBox.getSelectionModel().select(0);
        algorithmBox.setOnAction(e -> refresh());
        corridorsCheck.setOnAction(e -> refresh());
        riskCheck.setOnAction(e -> refresh());
        routesCheck.setOnAction(e -> refresh());
        civiliansCheck.setOnAction(e -> refresh());
        roadModeButton.setOnAction(e -> {
            pendingBlock = null;
            roadLabel.setText(roadModeButton.isSelected()
                    ? "Haga clic en dos celdas vecinas para bloquear o reabrir la vía entre ellas."
                    : "Active el modo para cerrar o reabrir tramos de vía.");
            refresh();
        });
        buildLegend();
    }

    @Override
    public void onShow() {
        canvasHolder.requestFocus();
    }

    @Override
    public void refresh() {
        DispatchCenter center = context.center();
        Position operator = center.operatorPosition();
        operatorLabel.setText("Posición " + operator + " · " + context.center().getMap().zoneAt(operator).getLabel());
        movementLabel.setText(center.operatorHistorySize() + " movimientos en la pila (se pueden deshacer)");
        if (corridorsCheck.isSelected()) {
            MstResult<Position> prim = center.primCorridors();
            MstResult<Position> kruskal = center.kruskalCorridors();
            corridorsLabel.setText(String.format(Locale.ROOT,
                    "Prim: %d tramos, costo %.1f\nKruskal: %d tramos, costo %.1f\n%s",
                    prim.getEdges().size(), prim.getTotalWeight(), kruskal.getEdges().size(), kruskal.getTotalWeight(),
                    Math.abs(prim.getTotalWeight() - kruskal.getTotalWeight()) < 1e-9
                            ? "Ambos algoritmos llegan al mismo costo total." : "Hay vías bloqueadas: el resultado es un bosque."));
        } else {
            corridorsLabel.setText("Red mínima que mantiene conectadas todas las zonas (sin congestión).");
        }
        redraw();
    }

    /** Keyboard control of the operator, active while the map is visible. */
    void handleKey(KeyEvent event) {
        if (event.getTarget() instanceof javafx.scene.control.TextInputControl) {
            return;
        }
        KeyCode code = event.getCode();
        Direction direction = null;
        if (code == KeyCode.W || code == KeyCode.UP) {
            direction = Direction.UP;
        } else if (code == KeyCode.S || code == KeyCode.DOWN) {
            direction = Direction.DOWN;
        } else if (code == KeyCode.A || code == KeyCode.LEFT) {
            direction = Direction.LEFT;
        } else if (code == KeyCode.D || code == KeyCode.RIGHT) {
            direction = Direction.RIGHT;
        }
        if (direction != null) {
            Direction chosen = direction;
            context.perform(null, () -> context.center().moveOperator(chosen));
            event.consume();
        } else if (code == KeyCode.U) {
            undo();
            event.consume();
        }
    }

    @FXML
    private void undo() {
        try {
            Movement undone = context.center().undoOperatorMovement();
            context.info("Movimiento deshecho: " + undone);
        } catch (SgmmsException empty) {
            context.error(empty.getMessage());
        }
        refresh();
    }

    private void handleClick(MouseEvent event) {
        canvasHolder.requestFocus();
        CityMap map = context.center().getMap();
        Position cell = renderer.cellAt(event.getX(), event.getY(), map);
        if (cell == null) {
            return;
        }
        if (roadModeButton.isSelected()) {
            toggleRoad(cell);
        } else {
            selected = cell;
            describeSelection(cell);
        }
        refresh();
    }

    private void toggleRoad(Position cell) {
        if (pendingBlock == null) {
            pendingBlock = cell;
            roadLabel.setText("Primera celda " + cell + ". Ahora elija una celda vecina.");
            return;
        }
        Position first = pendingBlock;
        pendingBlock = null;
        DispatchCenter center = context.center();
        if (center.getMap().isBlocked(first, cell)) {
            context.perform("Vía " + first + " - " + cell + " reabierta", () -> center.unblockRoad(first, cell));
        } else {
            context.perform(null, () -> {
                LinkedList<Incident> isolated = center.blockRoad(first, cell);
                if (isolated.isEmpty()) {
                    context.info("Vía " + first + " - " + cell + " bloqueada. Floyd-Warshall se recalculará.");
                } else {
                    context.error("Vía bloqueada. Incidentes sin vehículos que puedan llegar: " + isolated);
                }
            });
        }
        roadLabel.setText("Vías bloqueadas: " + center.blockedRoads().size() + ". Elija otra celda para continuar.");
    }

    private void describeSelection(Position cell) {
        DispatchCenter center = context.center();
        StringBuilder text = new StringBuilder("Celda " + cell + " · " + center.getMap().zoneAt(cell).getLabel());
        if (center.getMap().isRiskPoint(cell)) {
            text.append("\nPunto de riesgo (cruce de rutas principales)");
        }
        highlightedVehicle = null;
        for (Vehicle vehicle : center.vehicleList()) {
            if (vehicle.getPosition().equals(cell)) {
                text.append("\n").append(vehicle.getId()).append(" · ").append(vehicle.getType().getLabel())
                        .append(" · ").append(vehicle.getStatus().getLabel());
                if (vehicle.getAssignedIncidentId() != null) {
                    text.append(" → ").append(vehicle.getAssignedIncidentId());
                }
                highlightedVehicle = vehicle.getId();
            }
        }
        for (Incident incident : center.activeIncidentsByPriority()) {
            if (incident.getPosition().equals(cell)) {
                text.append("\n").append(incident.getId()).append(" · ").append(incident.getType().getLabel())
                        .append(" · gravedad ").append(incident.getSeverity().getLabel()).append(" · ")
                        .append(incident.getStatus().getLabel());
            }
        }
        selectionLabel.setText(text.toString());
    }

    private void redraw() {
        if (context == null || canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
            return;
        }
        DispatchCenter center = context.center();
        MapScene scene = new MapScene()
                .map(center.getMap())
                .blocked(center.blockedRoads())
                .vehicles(center.vehicleList())
                .incidents(center.activeIncidentsByPriority())
                .operator(center.operatorPosition())
                .selected(selected)
                .pendingBlock(pendingBlock)
                .highlightedVehicle(highlightedVehicle);
        if (corridorsCheck.isSelected()) {
            MstResult<Position> tree = "Kruskal".equals(algorithmBox.getValue())
                    ? center.kruskalCorridors() : center.primCorridors();
            scene.corridors(tree.getEdges());
        }
        if (riskCheck.isSelected()) {
            scene.riskPoints(center.riskPoints());
        }
        if (civiliansCheck.isSelected()) {
            scene.civilians(context.simulation().civilianPositions());
        }
        if (routesCheck.isSelected()) {
            for (Vehicle vehicle : center.vehicleList()) {
                if (vehicle.getStatus() == VehicleStatus.EN_ROUTE) {
                    scene.route(vehicle.getId(), center.remainingRoute(vehicle.getId()));
                }
            }
        }
        renderer.draw(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), scene);
    }

    private void buildLegend() {
        legendBox.getChildren().clear();
        for (ZoneType zone : ZoneType.values()) {
            Rectangle swatch = new Rectangle(16, 16, Palette.zone(zone));
            swatch.setStroke(Color.web("#cbd5e1"));
            legendBox.getChildren().add(row(swatch, zone.getLabel()));
        }
        for (IncidentType type : IncidentType.values()) {
            Polygon diamond = new Polygon(8, 0, 16, 8, 8, 16, 0, 8);
            diamond.setFill(Palette.incident(type));
            legendBox.getChildren().add(row(diamond, type.getLabel() + " (borde: gravedad)"));
        }
        for (VehicleType type : VehicleType.values()) {
            Circle circle = new Circle(8, Palette.vehicle(type));
            legendBox.getChildren().add(row(circle, type.getLabel() + " (" + Palette.vehicleLetter(type) + ")"));
        }
        Circle civilian = new Circle(4, Palette.CIVILIAN);
        legendBox.getChildren().add(row(civilian, "Vehículo particular"));
        Rectangle operator = new Rectangle(14, 14, Color.TRANSPARENT);
        operator.setStroke(Palette.OPERATOR);
        operator.setStrokeWidth(3);
        legendBox.getChildren().add(row(operator, "Operador (W A S D / flechas)"));
        Circle risk = new Circle(7, Color.TRANSPARENT);
        risk.setStroke(Palette.RISK);
        risk.setStrokeWidth(2);
        legendBox.getChildren().add(row(risk, "Punto de riesgo"));
        Rectangle corridor = new Rectangle(16, 4, Palette.CORRIDOR);
        legendBox.getChildren().add(row(corridor, "Corredor de emergencia (MST)"));
        Rectangle blocked = new Rectangle(4, 16, Palette.BLOCKED);
        legendBox.getChildren().add(row(blocked, "Vía bloqueada"));
        for (Severity severity : new Severity[] {Severity.HIGH, Severity.MEDIUM, Severity.LOW}) {
            Rectangle swatch = new Rectangle(16, 4, Palette.severity(severity));
            legendBox.getChildren().add(row(swatch, "Gravedad " + severity.getLabel().toLowerCase(Locale.ROOT)));
        }
    }

    private static HBox row(javafx.scene.Node icon, String text) {
        Pane holder = new Pane(icon);
        holder.setMinSize(18, 18);
        holder.setPrefSize(18, 18);
        Label label = new Label(text);
        label.getStyleClass().add("legend-text");
        HBox row = new HBox(8, holder, label);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}
