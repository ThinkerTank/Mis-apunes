package co.edu.icesi.sgmms.controller;

import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.simulation.Simulation;
import co.edu.icesi.sgmms.model.simulation.SimulationEngine;
import co.edu.icesi.sgmms.persistence.ConfigLoader;
import co.edu.icesi.sgmms.persistence.ConfigurationException;
import co.edu.icesi.sgmms.persistence.PersistenceException;
import co.edu.icesi.sgmms.persistence.SgmmsConfig;
import co.edu.icesi.sgmms.persistence.SimulationRepository;
import java.io.File;
import java.io.IOException;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Controller of the application shell: header with the system name, clock and score, the
 * navigation between the three scenarios, the simulation controls (start, pause, speed) and the
 * file operations (save and load a simulation, load a JSON configuration).
 */
public class MainController {

    private static final String[] SPEED_LABELS = {
        "Lenta · 2 s por minuto", "Normal · 1 s por minuto", "Rápida · 0,5 s por minuto", "Muy rápida · 0,25 s por minuto",
    };
    private static final long[] SPEED_MILLIS = {2000, 1000, 500, 250};

    @FXML private Label cityLabel;
    @FXML private Label clockLabel;
    @FXML private Label scoreLabel;
    @FXML private Button runButton;
    @FXML private ComboBox<String> speedBox;
    @FXML private ToggleButton monitoringTab;
    @FXML private ToggleButton mapTab;
    @FXML private ToggleButton panelTab;
    @FXML private StackPane content;
    @FXML private Label statusLabel;

    private final AppContext context = new AppContext();
    private final Node[] views = new Node[Screen.values().length];
    private final ScreenController[] controllers = new ScreenController[Screen.values().length];
    private final PauseTransition statusTimer = new PauseTransition(Duration.seconds(7));
    private Screen current;

    @FXML
    private void initialize() {
        ToggleGroup tabs = new ToggleGroup();
        monitoringTab.setToggleGroup(tabs);
        mapTab.setToggleGroup(tabs);
        panelTab.setToggleGroup(tabs);
        tabs.selectedToggleProperty().addListener((obs, before, now) -> {
            if (now == null && before != null) {
                before.setSelected(true);
            }
        });
        speedBox.getItems().addAll(SPEED_LABELS);
        speedBox.getSelectionModel().select(1);
        speedBox.setOnAction(e -> context.setMillisPerMinute(SPEED_MILLIS[speedBox.getSelectionModel().getSelectedIndex()]));
        statusTimer.setOnFinished(e -> statusLabel.setText(""));
        context.attach(this);
    }

    /**
     * Loads the three screens, installs the simulation and shows the monitoring center.
     *
     * @throws IOException if a view cannot be loaded
     */
    public void start(Scene scene, Simulation simulation) throws IOException {
        for (Screen screen : Screen.values()) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(screen.resource()));
            Parent view = loader.load();
            ScreenController controller = loader.getController();
            views[screen.ordinal()] = view;
            controllers[screen.ordinal()] = controller;
            controller.init(context);
            context.register(controller);
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (current == Screen.MAP) {
                ((TrafficMapController) controllers[Screen.MAP.ordinal()]).handleKey(event);
            }
        });
        context.install(simulation);
        show(Screen.MONITORING);
        updateRunButton();
        showStatus("Simulación lista y en pausa. Pulse «Iniciar» para que corra el tiempo.", false);
    }

    /** Shows one of the three scenarios. */
    public void show(Screen screen) {
        current = screen;
        content.getChildren().setAll(views[screen.ordinal()]);
        monitoringTab.setSelected(screen == Screen.MONITORING);
        mapTab.setSelected(screen == Screen.MAP);
        panelTab.setSelected(screen == Screen.PANEL);
        controllers[screen.ordinal()].onShow();
        controllers[screen.ordinal()].refresh();
    }

    /** Updates the clock and the score. */
    void refreshHeader(Indicators indicators) {
        cityLabel.setText("Sistema de Gestión y Monitoreo de Movilidad y Seguridad · " + context.simulation().getCityName());
        long minute = indicators.getMinute();
        long totalMinutes = 8 * 60 + minute;
        long day = totalMinutes / (24 * 60) + 1;
        long hour = (totalMinutes / 60) % 24;
        clockLabel.setText(String.format("Día %d · %02d:%02d  (min %d)", day, hour, totalMinutes % 60, minute));
        scoreLabel.setText(indicators.getScore() + " pts");
    }

    /** Shows a message in the status bar for a few seconds. */
    void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-error", "status-ok");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-ok");
        statusTimer.playFromStart();
    }

    public AppContext getContext() {
        return context;
    }

    // ---------------------------------------------------------------- header actions

    @FXML
    private void showMonitoring() {
        show(Screen.MONITORING);
    }

    @FXML
    private void showMap() {
        show(Screen.MAP);
    }

    @FXML
    private void showPanel() {
        show(Screen.PANEL);
    }

    @FXML
    private void toggleRun() {
        SimulationEngine engine = context.engine();
        if (engine.isPaused()) {
            engine.resume();
            showStatus("El tiempo corre. Los incidentes empezarán a aparecer.", false);
        } else {
            engine.pause();
            showStatus("Simulación en pausa: puede seguir asignando y resolviendo.", false);
        }
        updateRunButton();
    }

    private void updateRunButton() {
        boolean paused = context.engine().isPaused();
        runButton.setText(paused ? "▶  Iniciar" : "⏸  Pausar");
    }

    @FXML
    private void saveSimulation() {
        FileChooser chooser = chooser("Guardar simulación");
        chooser.setInitialFileName("simulacion.sgmms");
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            SimulationRepository.save(context.simulation(), file.toPath());
            showStatus("Simulación guardada en " + file.getName(), false);
        } catch (PersistenceException error) {
            alert("No se pudo guardar", error.getMessage());
        }
    }

    @FXML
    private void loadSimulation() {
        File file = chooser("Cargar simulación").showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            Simulation loaded = SimulationRepository.load(file.toPath());
            context.install(loaded);
            updateRunButton();
            show(current);
            showStatus("Simulación recuperada desde " + file.getName() + " (en pausa)", false);
        } catch (PersistenceException error) {
            alert("No se pudo cargar la simulación", error.getMessage());
        }
    }

    @FXML
    private void loadConfiguration() {
        FileChooser cityChooser = new FileChooser();
        cityChooser.setTitle("1/2 · Seleccione el archivo del mapa (city.json)");
        cityChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Configuración JSON", "*.json"));
        File city = cityChooser.showOpenDialog(window());
        if (city == null) {
            return;
        }
        FileChooser vehicleChooser = new FileChooser();
        vehicleChooser.setTitle("2/2 · Seleccione el archivo de vehículos (vehicles.json)");
        vehicleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Configuración JSON", "*.json"));
        vehicleChooser.setInitialDirectory(city.getParentFile());
        File vehicles = vehicleChooser.showOpenDialog(window());
        if (vehicles == null) {
            return;
        }
        try {
            SgmmsConfig config = ConfigLoader.load(city.toPath(), vehicles.toPath());
            context.install(config.createSimulation());
            updateRunButton();
            show(Screen.MONITORING);
            showStatus("Configuración cargada: " + config.getCityName(), false);
        } catch (ConfigurationException error) {
            alert("Configuración inválida", error.getMessage());
        }
    }

    @FXML
    private void restartDefault() {
        try {
            context.install(ConfigLoader.loadDefault().createSimulation());
            updateRunButton();
            show(Screen.MONITORING);
            showStatus("Nueva simulación con la configuración de Palmira", false);
        } catch (ConfigurationException error) {
            alert("Configuración inválida", error.getMessage());
        }
    }

    private FileChooser chooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Simulación SGMMS", "*.sgmms"));
        return chooser;
    }

    private void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(window());
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
        showStatus(message, true);
    }

    private Window window() {
        return content.getScene().getWindow();
    }
}
