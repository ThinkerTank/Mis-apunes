package co.edu.icesi.sgmms.controller;

import co.edu.icesi.sgmms.model.AssignmentRecord;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentStatus;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.OperatorAction;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.ScoreBreakdown;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.VehicleType;
import co.edu.icesi.sgmms.model.exceptions.SgmmsException;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import co.edu.icesi.sgmms.view.Palette;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Incident panel (scenario 3): register, look up and manage active incidents.
 *
 * <p>The table lists the active incidents in BST order (most urgent first) and can be filtered by
 * severity (a BST query). The top cards show the most urgent pending incident (top of the
 * priority queue, O(1)), the next one by arrival (front of the dispatch queue) and the last
 * administrative action (top of the action stack). Looking up by ID, assigning and resolving go
 * through the hash index. The lower tabs list the vehicles, the resolved incidents and the action
 * history, and two forms register new incidents and vehicles.
 */
public class IncidentPanelController implements ScreenController {

    @FXML private Label mostUrgentLabel;
    @FXML private Label nextInQueueLabel;
    @FXML private Label lastActionLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> severityFilter;
    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> idColumn;
    @FXML private TableColumn<Incident, String> typeColumn;
    @FXML private TableColumn<Incident, String> locationColumn;
    @FXML private TableColumn<Incident, String> severityColumn;
    @FXML private TableColumn<Incident, String> statusColumn;
    @FXML private TableColumn<Incident, String> vehicleColumn;
    @FXML private TableColumn<Incident, String> createdColumn;
    @FXML private TableColumn<Incident, String> elapsedColumn;
    @FXML private Label detailTitle;
    @FXML private Label detailBody;
    @FXML private Label suggestionLabel;
    @FXML private ComboBox<Vehicle> vehicleChoice;
    @FXML private Button assignButton;
    @FXML private Button assignSuggestedButton;
    @FXML private Button resolveButton;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> vIdColumn;
    @FXML private TableColumn<Vehicle, String> vTypeColumn;
    @FXML private TableColumn<Vehicle, String> vLocationColumn;
    @FXML private TableColumn<Vehicle, String> vStatusColumn;
    @FXML private TableColumn<Vehicle, String> vIncidentColumn;
    @FXML private TableView<Incident> resolvedTable;
    @FXML private TableColumn<Incident, String> rIdColumn;
    @FXML private TableColumn<Incident, String> rTypeColumn;
    @FXML private TableColumn<Incident, String> rSeverityColumn;
    @FXML private TableColumn<Incident, String> rMinutesColumn;
    @FXML private TableColumn<Incident, String> rVehicleColumn;
    @FXML private ListView<String> actionList;
    @FXML private ComboBox<IncidentType> newType;
    @FXML private ComboBox<Severity> newSeverity;
    @FXML private TextField newX;
    @FXML private TextField newY;
    @FXML private TextField newDescription;
    @FXML private TextField vehicleIdField;
    @FXML private ComboBox<VehicleType> vehicleTypeBox;
    @FXML private TextField vehicleX;
    @FXML private TextField vehicleY;

    private AppContext context;
    private String selectedId;

    /** Complexity — time O(1): configures a fixed number of columns and lists; space O(1). */
    @Override
    public void init(AppContext appContext) {
        this.context = appContext;
        column(idColumn, Incident::getId);
        column(typeColumn, i -> i.getType().getLabel());
        column(locationColumn, i -> i.getPosition().toString());
        column(severityColumn, i -> i.getSeverity().getLabel());
        column(statusColumn, i -> i.getStatus().getLabel());
        column(vehicleColumn, i -> i.getAssignedVehicleId() == null ? "—" : i.getAssignedVehicleId());
        column(createdColumn, i -> "min " + i.getGeneratedAtMinute() + " · " + i.getCreatedAtLabel().substring(11));
        column(elapsedColumn, i -> (context.center().currentMinute() - i.getGeneratedAtMinute()) + " min");
        severityColumn.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Incident incident = empty || getTableRow() == null ? null : (Incident) getTableRow().getItem();
                setStyle(incident == null ? "" : "-fx-text-fill: " + Palette.css(Palette.severity(incident.getSeverity()))
                        + "; -fx-font-weight: bold;");
            }
        });
        incidentTable.setPlaceholder(new Label("No hay incidentes activos."));
        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, before, now) -> {
            if (now != null) {
                selectedId = now.getId();
            }
            showDetails();
        });

        column(vIdColumn, Vehicle::getId);
        column(vTypeColumn, v -> v.getType().getLabel());
        column(vLocationColumn, v -> v.getPosition().toString());
        column(vStatusColumn, v -> v.getStatus().getLabel());
        column(vIncidentColumn, v -> v.getAssignedIncidentId() == null ? "—" : v.getAssignedIncidentId());
        column(rIdColumn, Incident::getId);
        column(rTypeColumn, i -> i.getType().getLabel());
        column(rSeverityColumn, i -> i.getSeverity().getLabel());
        column(rMinutesColumn, i -> i.attentionMinutes() + " min");
        column(rVehicleColumn, Incident::getAssignedVehicleId);
        resolvedTable.setPlaceholder(new Label("Aún no se ha resuelto ningún incidente."));

        severityFilter.getItems().addAll("Todas las gravedades", "Solo alta", "Solo media", "Solo baja");
        severityFilter.getSelectionModel().select(0);
        severityFilter.setOnAction(e -> refresh());

        vehicleChoice.setConverter(converter(v -> v.getId() + " · " + v.getType().getLabel() + " · " + v.getPosition()));
        newType.getItems().addAll(IncidentType.values());
        newType.setConverter(converter(IncidentType::getLabel));
        newType.getSelectionModel().select(IncidentType.ROBBERY);
        newSeverity.getItems().addAll(Severity.HIGH, Severity.MEDIUM, Severity.LOW);
        newSeverity.setConverter(converter(Severity::getLabel));
        newSeverity.getSelectionModel().select(Severity.MEDIUM);
        vehicleTypeBox.getItems().addAll(VehicleType.values());
        vehicleTypeBox.setConverter(converter(VehicleType::getLabel));
        vehicleTypeBox.getSelectionModel().select(VehicleType.PATROL);
    }

    @Override
    public void onShow() {
        // the panel is rebuilt by refresh()
    }

    /** Complexity — time O(n + m + r + k) for n active incidents, m vehicles, r resolved incidents and k actions, plus O(m log m) for the vehicle suggestion; space O(n + m + r + k) for the table rows. */
    @Override
    public void refresh() {
        if (incidentTable.getScene() == null) {
            return;
        }
        DispatchCenter center = context.center();
        mostUrgentLabel.setText(describe(center.mostUrgentPending()));
        nextInQueueLabel.setText(describe(center.nextInDispatchQueue()));
        lastActionLabel.setText(center.hasActions() ? center.lastAction().toString() : "—");

        LinkedList<Incident> active = filteredIncidents(center);
        ObservableList<Incident> rows = FXCollections.observableArrayList();
        Incident keep = null;
        for (Incident incident : active) {
            rows.add(incident);
            if (incident.getId().equals(selectedId)) {
                keep = incident;
            }
        }
        incidentTable.setItems(rows);
        if (keep != null) {
            incidentTable.getSelectionModel().select(keep);
        }

        Vehicle selectedVehicle = vehicleTable.getSelectionModel().getSelectedItem();
        ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
        for (Vehicle vehicle : center.vehicleList()) {
            vehicles.add(vehicle);
        }
        vehicleTable.setItems(vehicles);
        if (selectedVehicle != null) {
            vehicleTable.getSelectionModel().select(selectedVehicle);
        }

        ObservableList<Incident> resolved = FXCollections.observableArrayList();
        for (Incident incident : center.resolvedIncidents()) {
            resolved.add(0, incident);
        }
        resolvedTable.setItems(resolved);

        ObservableList<String> actions = FXCollections.observableArrayList();
        for (OperatorAction action : center.actionHistory()) {
            actions.add(action.toString());
        }
        actionList.setItems(actions);
        showDetails();
    }

    // ---------------------------------------------------------------- actions

    @FXML
    private void search() {
        String id = searchField.getText().trim().toUpperCase(Locale.ROOT);
        try {
            Incident found = context.center().findIncident(id);
            severityFilter.getSelectionModel().select(0);
            selectedId = found.getId();
            refresh();
            incidentTable.scrollTo(found);
            context.info("Incidente " + id + " encontrado en el índice hash");
        } catch (SgmmsException missing) {
            context.error(missing.getMessage());
        }
    }

    @FXML
    private void assignSelected() {
        Incident incident = selectedIncident();
        Vehicle vehicle = vehicleChoice.getValue();
        if (incident == null || vehicle == null) {
            context.error("Seleccione un incidente pendiente y un vehículo de la lista");
            return;
        }
        assign(incident.getId(), vehicle.getId());
    }

    @FXML
    private void assignSuggested() {
        Incident incident = selectedIncident();
        if (incident == null) {
            context.error("Seleccione un incidente pendiente");
            return;
        }
        Vehicle suggestion = context.center().suggestVehicle(incident.getId());
        if (suggestion == null) {
            context.error("No hay vehículos compatibles disponibles que puedan llegar a " + incident.getId());
            return;
        }
        assign(incident.getId(), suggestion.getId());
    }

    @FXML
    private void attendNext() {
        context.perform(null, () -> {
            AssignmentRecord decision = context.center().attendNext();
            selectedId = decision.getIncidentId();
            context.info(decision.getVehicleId() + " enviado a " + decision.getIncidentId()
                    + " (el más urgente) · llegada estimada " + decision.estimatedMinutes() + " min");
        });
    }

    @FXML
    private void resolve() {
        Incident incident = selectedIncident();
        if (incident == null) {
            context.error("Seleccione el incidente que desea resolver");
            return;
        }
        context.perform(null, () -> {
            ScoreBreakdown points = context.center().resolveIncident(incident.getId());
            context.info(incident.getId() + " resuelto: +" + points);
        });
    }

    @FXML
    private void useOperatorPosition() {
        Position here = context.center().operatorPosition();
        newX.setText(String.valueOf(here.getX()));
        newY.setText(String.valueOf(here.getY()));
    }

    @FXML
    private void registerIncident() {
        Position position = readPosition(newX, newY);
        if (position == null) {
            return;
        }
        String description = newDescription.getText().isBlank() ? "Reportado por el operador" : newDescription.getText().trim();
        context.perform(null, () -> {
            Incident created = context.center().reportIncident(newType.getValue(), position, newSeverity.getValue(), description);
            selectedId = created.getId();
            newDescription.clear();
            context.info("Incidente " + created.getId() + " registrado: entra a la cola de despacho y al árbol de prioridad");
        });
    }

    @FXML
    private void registerVehicle() {
        Position position = readPosition(vehicleX, vehicleY);
        if (position == null) {
            return;
        }
        context.perform("Vehículo " + vehicleIdField.getText().trim() + " registrado",
                () -> context.center().registerVehicle(vehicleIdField.getText(), vehicleTypeBox.getValue(), position));
    }

    @FXML
    private void toggleOutOfService() {
        Vehicle vehicle = vehicleTable.getSelectionModel().getSelectedItem();
        if (vehicle == null) {
            context.error("Seleccione un vehículo en la tabla");
            return;
        }
        boolean takeOut = vehicle.getStatus() == VehicleStatus.AVAILABLE;
        context.perform(vehicle.getId() + (takeOut ? " fuera de servicio" : " de vuelta en servicio"),
                () -> context.center().setOutOfService(vehicle.getId(), takeOut));
    }

    // ---------------------------------------------------------------- helpers

    private void assign(String incidentId, String vehicleId) {
        context.perform(null, () -> {
            AssignmentRecord decision = context.center().assignVehicle(incidentId, vehicleId);
            context.info(String.format(Locale.ROOT, "%s enviado a %s · ruta Dijkstra %.1f · llegada estimada %d min%s",
                    vehicleId, incidentId, decision.getChosenRouteCost(), decision.estimatedMinutes(),
                    decision.choseCheapestRoute() ? " · la mejor opción disponible" : ""));
        });
    }

    private LinkedList<Incident> filteredIncidents(DispatchCenter center) {
        switch (severityFilter.getSelectionModel().getSelectedIndex()) {
            case 1:
                return center.activeIncidentsWithSeverity(Severity.HIGH);
            case 2:
                return center.activeIncidentsWithSeverity(Severity.MEDIUM);
            case 3:
                return center.activeIncidentsWithSeverity(Severity.LOW);
            default:
                return center.activeIncidentsByPriority();
        }
    }

    private Incident selectedIncident() {
        return incidentTable.getSelectionModel().getSelectedItem();
    }

    private void showDetails() {
        Incident incident = selectedIncident();
        DispatchCenter center = context.center();
        if (incident == null) {
            detailTitle.setText("Seleccione un incidente");
            detailBody.setText("Elija una fila de la tabla para ver su información y asignarle un vehículo.");
            suggestionLabel.setText("");
            vehicleChoice.getItems().clear();
            assignButton.setDisable(true);
            assignSuggestedButton.setDisable(true);
            resolveButton.setDisable(true);
            return;
        }
        long elapsed = center.currentMinute() - incident.getGeneratedAtMinute();
        StringBuilder body = new StringBuilder();
        body.append("Tipo: ").append(incident.getType().getLabel())
                .append("   ·   Gravedad: ").append(incident.getSeverity().getLabel())
                .append("   ·   Estado: ").append(incident.getStatus().getLabel())
                .append("\nUbicación: ").append(incident.getPosition()).append(" · ")
                .append(center.getMap().zoneAt(incident.getPosition()).getLabel())
                .append("\nGenerado: ").append(incident.getCreatedAtLabel()).append(" (minuto ")
                .append(incident.getGeneratedAtMinute()).append(") · hace ").append(elapsed).append(" min")
                .append("\nTiempo máximo para la bonificación: ").append(incident.getSeverity().getMaxAttentionMinutes()).append(" min")
                .append("\nDescripción: ").append(incident.getDescription());
        if (incident.getAssignedVehicleId() != null) {
            Vehicle vehicle = center.findVehicle(incident.getAssignedVehicleId());
            body.append("\nVehículo asignado: ").append(vehicle.getId()).append(" · ").append(vehicle.getStatus().getLabel());
            if (vehicle.getStatus() == VehicleStatus.EN_ROUTE) {
                body.append(" · ").append(vehicle.remainingSteps()).append(" celdas por recorrer");
            }
            AssignmentRecord decision = center.assignmentOf(incident.getId());
            if (decision != null) {
                body.append(String.format(Locale.ROOT, "\nRuta Dijkstra: %.1f (mejor disponible %.1f) · Floyd-Warshall: %.1f (óptimo %.1f)",
                        decision.getChosenRouteCost(), decision.getBestRouteCost(),
                        decision.getChosenStructuralDistance(), decision.getBestStructuralDistance()));
            }
        }
        detailTitle.setText(incident.getId() + " · " + incident.getType().getLabel());
        detailBody.setText(body.toString());

        boolean pending = incident.getStatus() == IncidentStatus.PENDING;
        Vehicle assigned = incident.getAssignedVehicleId() == null ? null : center.findVehicle(incident.getAssignedVehicleId());
        assignButton.setDisable(!pending);
        assignSuggestedButton.setDisable(!pending);
        resolveButton.setDisable(assigned == null || assigned.getStatus() != VehicleStatus.ATTENDING);
        if (pending) {
            Vehicle suggestion = center.suggestVehicle(incident.getId());
            suggestionLabel.setText(suggestion == null ? "No hay vehículos compatibles disponibles que puedan llegar."
                    : String.format(Locale.ROOT, "Sugerido: %s · %s a %.1f de distancia (Floyd-Warshall)",
                    suggestion.getId(), suggestion.getType().getLabel(),
                    center.structuralDistance(suggestion.getPosition(), incident.getPosition())));
            Vehicle chosen = vehicleChoice.getValue();
            vehicleChoice.getItems().clear();
            for (Vehicle vehicle : center.vehicleList()) {
                if (vehicle.isAvailable() && vehicle.canAttend(incident.getType())) {
                    vehicleChoice.getItems().add(vehicle);
                }
            }
            if (chosen != null && vehicleChoice.getItems().contains(chosen)) {
                vehicleChoice.setValue(chosen);
            } else if (suggestion != null) {
                vehicleChoice.setValue(suggestion);
            }
        } else {
            suggestionLabel.setText(assigned != null && assigned.getStatus() == VehicleStatus.ATTENDING
                    ? "El vehículo ya está en el lugar: puede resolver el incidente."
                    : "El vehículo va en camino; podrá resolverlo cuando llegue.");
            vehicleChoice.getItems().clear();
        }
    }

    private Position readPosition(TextField xField, TextField yField) {
        try {
            return new Position(Integer.parseInt(xField.getText().trim()), Integer.parseInt(yField.getText().trim()));
        } catch (NumberFormatException notANumber) {
            context.error("Las coordenadas X y Y deben ser números enteros");
            return null;
        }
    }

    private static String describe(Incident incident) {
        if (incident == null) {
            return "—";
        }
        return incident.getId() + " · " + incident.getType().getLabel() + " · " + incident.getSeverity().getLabel()
                + " · " + incident.getPosition();
    }

    private static <S> void column(TableColumn<S, String> column, Function<S, String> value) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    }

    private static <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : toText.apply(object);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }
}
