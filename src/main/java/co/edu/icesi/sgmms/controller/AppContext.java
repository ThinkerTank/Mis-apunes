package co.edu.icesi.sgmms.controller;

import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.SimulationEvent;
import co.edu.icesi.sgmms.model.exceptions.SgmmsException;
import co.edu.icesi.sgmms.model.simulation.Simulation;
import co.edu.icesi.sgmms.model.simulation.SimulationEngine;
import co.edu.icesi.sgmms.model.simulation.SimulationListener;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * State shared by the controllers: the running simulation, its engine, and the channel through
 * which the screens are refreshed and messages are shown.
 *
 * <p>The engine calls back from its indicator thread; this class moves every call onto the
 * JavaFX thread with {@link Platform#runLater}, the only thread allowed to touch the interface.
 * The activity list is a JavaFX {@link ObservableList} only because list views require one; the
 * model's own data stays in the project's structures.
 */
public class AppContext {

    private static final int ACTIVITY_LIMIT = 300;
    private static final long DEFAULT_MILLIS_PER_MINUTE = 1000;

    private final ObservableList<String> activity = FXCollections.observableArrayList();
    private final LinkedList<ScreenController> screens = new LinkedList<>();
    private MainController main;
    private Simulation simulation;
    private SimulationEngine engine;
    private long millisPerMinute = DEFAULT_MILLIS_PER_MINUTE;

    void attach(MainController mainController) {
        this.main = mainController;
    }

    void register(ScreenController screen) {
        screens.addLast(screen);
    }

    /**
     * Replaces the current simulation (startup, loading a save or a new configuration). The old
     * engine is stopped; the new one starts paused so the operator can look around first.
     *
     * <p>Complexity — time O(h) to copy the h events of the saved history into the activity list; space O(h).
     */
    public void install(Simulation newSimulation) {
        if (engine != null) {
            engine.stop();
        }
        simulation = newSimulation;
        engine = new SimulationEngine(simulation, new SimulationListener() {
            @Override
            public void onUpdate(Indicators indicators, LinkedList<SimulationEvent> events) {
                Platform.runLater(() -> applyUpdate(events));
            }

            @Override
            public void onError(Throwable error) {
                Platform.runLater(() -> error("Error en la simulación: " + error.getMessage()));
            }
        }, millisPerMinute);
        activity.clear();
        for (SimulationEvent event : simulation.getCenter().eventHistory()) {
            activity.add(0, event.toString());
        }
        simulation.getCenter().pollEvents();
        engine.start();
        engine.pause();
        refreshAll();
    }

    /** Applies the events drained by the indicator thread and refreshes every screen. */
    private void applyUpdate(LinkedList<SimulationEvent> events) {
        for (SimulationEvent event : events) {
            activity.add(0, event.toString());
        }
        while (activity.size() > ACTIVITY_LIMIT) {
            activity.remove(activity.size() - 1);
        }
        refreshAll();
    }

    /**
     * Refreshes the header and every screen from the model (JavaFX thread only).
     *
     * <p>Complexity — time O(n + m + V + E) per call: every screen rebuilds itself from model snapshots (n incidents, m vehicles, BFS/DFS over the map).
     */
    public void refreshAll() {
        if (simulation == null) {
            return;
        }
        if (main != null) {
            main.refreshHeader(simulation.getCenter().indicators());
        }
        for (ScreenController screen : screens) {
            screen.refresh();
        }
    }

    /**
     * Runs an operator action, shows its result, and turns any domain error into a message.
     *
     * <p>Complexity — the cost of {@code action} plus O(k) to drain k pending events and one full refresh.
     */
    public void perform(String successMessage, Runnable action) {
        try {
            action.run();
            if (successMessage != null) {
                info(successMessage);
            }
        } catch (SgmmsException rejected) {
            error(rejected.getMessage());
        } catch (RuntimeException unexpected) {
            error("Operación no realizada: " + unexpected.getMessage());
        }
        applyUpdate(simulation.getCenter().pollEvents());
    }

    /** Complexity — time O(1), space O(1). */
    public void info(String message) {
        if (main != null) {
            main.showStatus(message, false);
        }
    }

    /** Complexity — time O(1), space O(1). */
    public void error(String message) {
        if (main != null) {
            main.showStatus(message, true);
        }
    }

    public void navigate(Screen screen) {
        if (main != null) {
            main.show(screen);
        }
    }

    public Simulation simulation() {
        return simulation;
    }

    public DispatchCenter center() {
        return simulation.getCenter();
    }

    public SimulationEngine engine() {
        return engine;
    }

    public ObservableList<String> activity() {
        return activity;
    }

    public void setMillisPerMinute(long millis) {
        millisPerMinute = millis;
        if (engine != null) {
            engine.setMillisPerMinute(millis);
        }
    }

    public long getMillisPerMinute() {
        return millisPerMinute;
    }

    /** Stops the engine when the window closes. */
    public void shutdown() {
        if (engine != null) {
            engine.stop();
        }
    }
}
