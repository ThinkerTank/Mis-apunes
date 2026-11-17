package co.edu.icesi.sgmms.model.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.exceptions.InvalidIncidentStateException;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Indicators;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.SimulationEvent;
import co.edu.icesi.sgmms.model.exceptions.SgmmsException;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    @Test
    void producersConsumersAndTheClockKeepTheSharedStructuresConsistent() throws Exception {
        DispatchCenter center = SimFixtures.center();
        LinkedList<Position> cells = center.getMap().traversableCells();
        int producers = 4;
        int perProducer = 50;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        AtomicInteger resolved = new AtomicInteger();
        Thread[] threads = new Thread[producers + 3];

        for (int p = 0; p < producers; p++) {
            long seed = p;
            threads[p] = new Thread(() -> guarded(unexpected, start, () -> {
                Random random = new Random(seed);
                IncidentType[] types = IncidentType.values();
                for (int i = 0; i < perProducer; i++) {
                    center.reportIncident(types[random.nextInt(types.length)],
                            cells.get(random.nextInt(cells.size())), Severity.values()[random.nextInt(3)], "carga");
                }
            }));
        }
        for (int c = 0; c < 2; c++) {
            threads[producers + c] = new Thread(() -> guarded(unexpected, start, () -> {
                for (int i = 0; i < 400; i++) {
                    try {
                        String id = center.attendNext().getIncidentId();
                        try {
                            center.startAttention(id);
                        } catch (InvalidIncidentStateException clockAlreadyBroughtItThere) {
                            // the clock thread moved the vehicle to the incident first
                        }
                        center.resolveIncident(id);
                        resolved.incrementAndGet();
                    } catch (SgmmsException expected) {
                        Thread.yield();
                    }
                }
            }));
        }
        threads[producers + 2] = new Thread(() -> guarded(unexpected, start, () -> {
            for (int i = 0; i < 200; i++) {
                center.advanceMinute();
            }
        }));

        for (Thread thread : threads) {
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join(30_000);
        }

        assertEquals(null, unexpected.get());
        assertTrue(center.isConsistent());
        Indicators totals = center.indicators();
        assertEquals(producers * perProducer, totals.getActiveIncidents() + totals.getResolvedIncidents());
        assertEquals(resolved.get(), totals.getResolvedIncidents());
        assertEquals(200, center.currentMinute());
    }

    @Test
    void engineRunsPausesResumesAndStopsItsThreads() throws Exception {
        Simulation simulation = new Simulation(SimFixtures.center(), SimFixtures.settings(4));
        AtomicInteger updates = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SimulationEngine engine = new SimulationEngine(simulation, new SimulationListener() {
            @Override
            public void onUpdate(Indicators indicators, LinkedList<SimulationEvent> events) {
                updates.incrementAndGet();
            }

            @Override
            public void onError(Throwable error) {
                failure.set(error);
            }
        }, 15);
        DispatchCenter center = simulation.getCenter();

        engine.start();
        waitUntil(() -> center.currentMinute() >= 5);
        engine.pause();
        Thread.sleep(60);
        long frozen = center.currentMinute();
        Thread.sleep(150);
        assertTrue(center.currentMinute() <= frozen + 1);

        engine.resume();
        waitUntil(() -> center.currentMinute() >= frozen + 5);
        engine.stop();

        assertFalse(engine.isRunning());
        assertTrue(engine.threadsFinished());
        assertTrue(updates.get() > 0);
        assertEquals(null, failure.get());
    }

    private static void guarded(AtomicReference<Throwable> unexpected, CountDownLatch start, Runnable body) {
        try {
            start.await();
            body.run();
        } catch (Throwable error) {
            unexpected.compareAndSet(null, error);
        }
    }

    private interface Condition {
        boolean holds();
    }

    private static void waitUntil(Condition condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!condition.holds() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.holds(), "condition not reached in time");
    }
}
