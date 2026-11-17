package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;
import java.util.Random;

/**
 * The simulated city: dispatch center, private traffic, incident generator and accident detector.
 * This is the object saved and restored with serialization.
 *
 * <p>The two time-driven operations are separate so they can run on different threads:
 * {@link #clockTick()} (time passes, vehicles move, accidents are detected) and
 * {@link #generationTick()} (robberies and fires appear). Both lock this object and then, inside,
 * the {@link DispatchCenter}; the center never calls back into this class, so locks are always
 * taken in the same order and cannot deadlock.
 */
public class Simulation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DispatchCenter center;
    private final SimulationSettings settings;
    private final Random random;
    private final CivilianTraffic traffic;
    private final IncidentGenerator generator = new IncidentGenerator();
    private final AccidentDetector detector;

    public Simulation(DispatchCenter center, SimulationSettings settings) {
        this.center = center;
        this.settings = settings;
        this.random = new Random(settings.getSeed());
        this.traffic = new CivilianTraffic(center.getMap(), settings.getCivilianCars(), random);
        this.detector = new AccidentDetector(settings.getAccidentCooldownMinutes());
        center.updateTraffic(traffic.positions());
    }

    /**
     * One simulated minute: the clock advances, emergency vehicles move along their routes,
     * private cars drive, the three accident criteria are evaluated over every movement, and road
     * weights are updated with the new congestion.
     *
     * <p>Complexity — time: O(m + c + V + E) per minute, plus Dijkstra for any rerouted vehicle;
     * space: O(m + c).
     *
     * @return the accidents generated this minute
     */
    public synchronized LinkedList<AccidentReport> clockTick() {
        HashTable<String, Position> before = new HashTable<>();
        for (Vehicle vehicle : center.vehicleList()) {
            if (vehicle.getStatus() == VehicleStatus.EN_ROUTE) {
                before.put(vehicle.getId(), vehicle.getPosition());
            }
        }
        center.advanceMinute();
        CityMap map = center.getMap();
        LinkedList<CarMove> moves = traffic.step(map, random);
        for (Vehicle vehicle : center.vehicleList()) {
            Position from = before.get(vehicle.getId());
            if (from != null) {
                moves.addLast(new CarMove(vehicle.getId(), from, vehicle.getPosition()));
            }
        }
        LinkedList<AccidentReport> accidents = new LinkedList<>();
        if (center.indicators().getActiveIncidents() < settings.getMaxActiveIncidents()) {
            accidents = detector.detect(map, moves, busyCells(), center.currentMinute());
            for (AccidentReport accident : accidents) {
                center.reportIncident(IncidentType.ACCIDENT, accident.getPosition(),
                        accident.getCriterion().getSeverity(), accident.description());
                for (String id : accident.getInvolved()) {
                    traffic.stop(id);
                }
            }
        }
        center.updateTraffic(traffic.positions());
        return accidents;
    }

    /**
     * Gives the generator its chance to create a robbery and a fire.
     *
     * <p>Complexity — time: O(n) plus O(V) the first time; space: O(n).
     */
    public synchronized LinkedList<Incident> generationTick() {
        return generator.tick(center, settings, random, busyCells());
    }

    /** Current positions of the private cars, for drawing them. Complexity — O(c). */
    public synchronized LinkedList<Position> civilianPositions() {
        return traffic.positions();
    }

    public DispatchCenter getCenter() {
        return center;
    }

    public SimulationSettings getSettings() {
        return settings;
    }

    private HashTable<Position, Boolean> busyCells() {
        HashTable<Position, Boolean> busy = new HashTable<>();
        for (Incident incident : center.activeIncidentsByPriority()) {
            busy.put(incident.getPosition(), Boolean.TRUE);
        }
        return busy;
    }
}
