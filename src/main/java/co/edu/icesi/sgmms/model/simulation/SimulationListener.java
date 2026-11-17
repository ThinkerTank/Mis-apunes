package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.SimulationEvent;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/**
 * Receives the results of the indicator thread. Implementations are called from a background
 * thread, so an interface must hand the data over to its own thread (in JavaFX, with
 * {@code Platform.runLater}).
 */
public interface SimulationListener {

    /** New indicator snapshot and the events produced since the previous call. */
    void onUpdate(Indicators indicators, LinkedList<SimulationEvent> events);

    /** An unexpected error inside a simulation thread; the thread keeps running. */
    void onError(Throwable error);
}
