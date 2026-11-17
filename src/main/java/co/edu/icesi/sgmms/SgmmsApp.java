package co.edu.icesi.sgmms;

import co.edu.icesi.sgmms.controller.MainController;
import co.edu.icesi.sgmms.model.simulation.Simulation;
import co.edu.icesi.sgmms.persistence.ConfigLoader;
import co.edu.icesi.sgmms.persistence.ConfigurationException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Entry point of the SGMMS prototype. Loads the bundled configuration of Palmira, builds the
 * simulation and opens the main window on the monitoring center.
 */
public class SgmmsApp extends Application {

    private MainController main;

    /** Complexity — time O(V²), dominated by building the adjacency matrix of the configured map; space O(V²). */
    @Override
    public void start(Stage stage) throws Exception {
        Simulation simulation;
        try {
            simulation = ConfigLoader.loadDefault().createSimulation();
        } catch (ConfigurationException invalid) {
            Alert alert = new Alert(Alert.AlertType.ERROR, invalid.getMessage());
            alert.setHeaderText("No se pudo cargar la configuración inicial");
            alert.showAndWait();
            Platform.exit();
            return;
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/icesi/sgmms/view/MainView.fxml"));
        Parent root = loader.load();
        main = loader.getController();
        Scene scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/co/edu/icesi/sgmms/view/styles.css").toExternalForm());
        stage.setTitle("SGMMS · Sistema de Gestión y Monitoreo de Movilidad y Seguridad");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        main.start(scene, simulation);
        stage.show();
    }

    /** Complexity — O(1) work; waits for the three simulation threads to finish (bounded by 2 s each). */
    @Override
    public void stop() {
        if (main != null) {
            main.getContext().shutdown();
        }
    }

    /** Complexity — time O(1), space O(1). */
    public static void main(String[] args) {
        launch(args);
    }
}
