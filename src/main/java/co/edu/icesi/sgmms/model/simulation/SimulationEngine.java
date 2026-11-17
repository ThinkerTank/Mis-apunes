package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.SimulationEvent;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/**
 * Runs the simulation with three concurrent threads (RF20):
 * <ol>
 *   <li><b>Clock</b> ({@code sgmms-clock}) — every simulated minute calls
 *       {@link Simulation#clockTick()}: time passes, vehicles move and arrive, private cars drive
 *       and accidents are detected.</li>
 *   <li><b>Generator</b> ({@code sgmms-generator}) — every simulated minute, half a period after
 *       the clock, calls {@link Simulation#generationTick()} to create robberies and fires.</li>
 *   <li><b>Indicators</b> ({@code sgmms-indicators}) — four times per second takes an
 *       {@link Indicators} snapshot and drains the event queue for the listener (the interface).</li>
 * </ol>
 *
 * <p><b>Shared resource and synchronization.</b> All three threads, plus the JavaFX thread when
 * the operator assigns or resolves, work on the same {@link DispatchCenter}. Its methods are
 * {@code synchronized}, so, for example, the generator inserting an incident into the BST and the
 * hash index, and the operator resolving another one and removing it from both, never interleave.
 * The event queue follows a producer/consumer scheme: the clock, the generator and the operator
 * produce events; only the indicator thread consumes them. The {@code running} and {@code paused}
 * flags are {@code volatile} so every thread sees changes immediately, and paused threads wait on
 * a monitor instead of spinning.
 */
public class SimulationEngine {

    private static final long INDICATOR_PERIOD_MILLIS = 250;

    private final Simulation simulation;
    private final SimulationListener listener;
    private final Object pauseLock = new Object();
    private volatile long millisPerMinute;
    private volatile boolean running;
    private volatile boolean paused;
    private Thread clock;
    private Thread generator;
    private Thread indicators;

    /**
     * @param millisPerMinute real milliseconds that one simulated minute lasts
     */
    public SimulationEngine(Simulation simulation, SimulationListener listener, long millisPerMinute) {
        this.simulation = simulation;
        this.listener = listener;
        this.millisPerMinute = Math.max(10, millisPerMinute);
    }

    /** Starts the three threads. Calling it twice has no effect. */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        paused = false;
        clock = daemon("sgmms-clock", () -> loop(0, simulation::clockTick));
        generator = daemon("sgmms-generator", () -> loop(millisPerMinute / 2, simulation::generationTick));
        indicators = daemon("sgmms-indicators", this::publishLoop);
        clock.start();
        generator.start();
        indicators.start();
    }

    /** Freezes time; the operator can still assign, resolve and move. */
    public void pause() {
        paused = true;
    }

    /** Lets time run again after {@link #pause()}. */
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    /** Stops every thread and waits for them to finish. */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        resume();
        for (Thread thread : new Thread[] {clock, generator, indicators}) {
            thread.interrupt();
        }
        for (Thread thread : new Thread[] {clock, generator, indicators}) {
            try {
                thread.join(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    /** Changes the speed: real milliseconds per simulated minute (minimum 10). */
    public void setMillisPerMinute(long millis) {
        this.millisPerMinute = Math.max(10, millis);
    }

    public long getMillisPerMinute() {
        return millisPerMinute;
    }

    /** Whether every thread has finished (used by the tests). */
    public boolean threadsFinished() {
        return (clock == null || !clock.isAlive()) && (generator == null || !generator.isAlive())
                && (indicators == null || !indicators.isAlive());
    }

    public Simulation getSimulation() {
        return simulation;
    }

    private void loop(long initialDelay, Runnable work) {
        try {
            Thread.sleep(initialDelay);
            while (running) {
                Thread.sleep(millisPerMinute);
                waitWhilePaused();
                if (!running) {
                    break;
                }
                try {
                    work.run();
                } catch (RuntimeException error) {
                    listener.onError(error);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void publishLoop() {
        DispatchCenter center = simulation.getCenter();
        try {
            while (running) {
                Thread.sleep(INDICATOR_PERIOD_MILLIS);
                Indicators snapshot = center.indicators();
                LinkedList<SimulationEvent> events = center.pollEvents();
                try {
                    listener.onUpdate(snapshot, events);
                } catch (RuntimeException error) {
                    listener.onError(error);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void waitWhilePaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && running) {
                pauseLock.wait();
            }
        }
    }

    private static Thread daemon(String name, Runnable body) {
        Thread thread = new Thread(body, name);
        thread.setDaemon(true);
        return thread;
    }
}
