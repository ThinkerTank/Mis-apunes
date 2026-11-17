package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.DuplicateVehicleException;
import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import co.edu.icesi.sgmms.model.exceptions.IncompatibleVehicleException;
import co.edu.icesi.sgmms.model.exceptions.InvalidIncidentStateException;
import co.edu.icesi.sgmms.model.exceptions.InvalidMapException;
import co.edu.icesi.sgmms.model.exceptions.RouteUnavailableException;
import co.edu.icesi.sgmms.model.exceptions.VehicleNotAvailableException;
import co.edu.icesi.sgmms.model.structures.AllPairsResult;
import co.edu.icesi.sgmms.model.structures.DfsResult;
import co.edu.icesi.sgmms.model.structures.Edge;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import co.edu.icesi.sgmms.model.structures.MstResult;
import co.edu.icesi.sgmms.model.structures.PriorityQueue;
import co.edu.icesi.sgmms.model.structures.Queue;
import co.edu.icesi.sgmms.model.structures.ShortestPathResult;
import co.edu.icesi.sgmms.model.structures.Stack;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Entry point of the model: the state of the whole simulation and every operation the operator
 * can perform. Controllers talk only to this class.
 *
 * <p><b>Structures it coordinates.</b>
 * <ul>
 *   <li>{@link IncidentRepository}: BST by priority + hash index by ID (always consistent).</li>
 *   <li>{@link Queue} {@code dispatchQueue}: pending incidents in arrival order — "who arrived
 *       first?" (RF23).</li>
 *   <li>{@link PriorityQueue} {@code attentionQueue}: pending incidents by severity and age —
 *       "which is the most urgent?" in O(1), and "attend next" extracts it in O(log n) (RF27).</li>
 *   <li>{@link Queue} {@code events}: simulation events consumed by the interface (RF24).</li>
 *   <li>{@link Stack} {@code actions}: administrative actions; the panel shows the top (RF22).</li>
 *   <li>{@link VehicleRepository}: hash table of vehicles by ID (RF25).</li>
 * </ul>
 *
 * <p><b>Concurrency.</b> This object is the resource shared by the simulation threads (clock,
 * incident generator, indicator updater) and the interface thread. Every public method is
 * {@code synchronized}, so each operation runs atomically: for example, a generator thread
 * inserting into the BST and the hash index can never interleave with the interface thread
 * resolving an incident and removing it from both. Read methods return copies or immutable
 * snapshots so callers never iterate a structure that another thread is modifying.
 */
public class DispatchCenter implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HISTORY_LIMIT = 300;

    private final CityMap map;
    private final IncidentRepository incidents = new IncidentRepository();
    private final VehicleRepository vehicles;
    private final OperatorNavigator operator;
    private final Queue<Incident> dispatchQueue = new Queue<>();
    private final PriorityQueue<Incident> attentionQueue = new PriorityQueue<>(new IncidentPriorityComparator());
    private final Queue<SimulationEvent> events = new Queue<>();
    private final LinkedList<SimulationEvent> history = new LinkedList<>();
    private final Stack<OperatorAction> actions = new Stack<>();
    private final HashTable<String, AssignmentRecord> assignments = new HashTable<>();
    private final ScoreCalculator scoring = new ScoreCalculator();
    private transient RouteService routes;
    private int score;
    private long minute;
    private long sequence;

    /**
     * @throws InvalidMapException if a vehicle is not on a traversable cell of the map
     */
    public DispatchCenter(CityMap map, VehicleRepository vehicles) {
        this.map = map;
        this.vehicles = vehicles;
        for (Vehicle vehicle : vehicles.all()) {
            if (!map.canEnter(vehicle.getPosition())) {
                throw new InvalidMapException("El vehículo " + vehicle.getId() + " no está sobre una vía: " + vehicle.getPosition());
            }
        }
        this.operator = new OperatorNavigator(map, map.getMonitoringCenter());
    }

    // ================================================================= incidents

    /**
     * Registers a new incident in the BST, the hash index, the dispatch queue and the attention
     * queue, and enqueues the corresponding event.
     *
     * <p>Complexity — time: average O(log n) (BST and heap) + O(1) (hash, queues); worst O(n)
     * for a degenerate tree; space: O(1).
     *
     * @throws InvalidMapException if the position is not a traversable cell
     */
    public synchronized Incident reportIncident(IncidentType type, Position position, Severity severity,
                                                String description) {
        if (!map.canEnter(position)) {
            throw new InvalidMapException("Los incidentes deben ocurrir en una celda transitable: " + position);
        }
        sequence++;
        String id = String.format("INC-%04d", sequence);
        Incident incident = new Incident(id, type, position, severity, description, LocalDateTime.now(), minute, sequence);
        incidents.add(incident);
        dispatchQueue.enqueue(incident);
        attentionQueue.insert(incident);
        record(SimulationEvent.Type.INCIDENT_CREATED, type.getLabel() + " " + id + " (" + severity.getLabel()
                + ") en " + position + ": " + description);
        return incident;
    }

    /** Active incident by ID (hash index, O(1) expected). @throws IncidentNotFoundException */
    public synchronized Incident findIncident(String id) {
        return incidents.findById(id);
    }

    /** Active incidents from the most to the least urgent (BST reverse in-order). Complexity — O(n). */
    public synchronized LinkedList<Incident> activeIncidentsByPriority() {
        return incidents.activeByPriority();
    }

    /** Active incident of highest priority according to the BST, or {@code null}. */
    public synchronized Incident mostUrgentActive() {
        return incidents.mostUrgent();
    }

    /**
     * Pending incident of highest priority: the top of the attention heap, or {@code null}.
     *
     * <p>Complexity — time: O(1); space: O(1).
     */
    public synchronized Incident mostUrgentPending() {
        return attentionQueue.isEmpty() ? null : attentionQueue.peekMax();
    }

    /**
     * Pending incident that arrived first: the front of the dispatch queue, or {@code null}.
     *
     * <p>Complexity — time: O(1); space: O(1).
     */
    public synchronized Incident nextInDispatchQueue() {
        return dispatchQueue.isEmpty() ? null : dispatchQueue.front();
    }

    /** Copy of the dispatch queue, front first. Complexity — O(n). */
    public synchronized LinkedList<Incident> dispatchQueueSnapshot() {
        LinkedList<Incident> copy = new LinkedList<>();
        for (Incident incident : dispatchQueue) {
            copy.addLast(incident);
        }
        return copy;
    }

    /** Copy of the resolved incidents. Complexity — O(r). */
    public synchronized LinkedList<Incident> resolvedIncidents() {
        return incidents.resolvedHistory().copy();
    }

    // ================================================================= vehicles and assignment

    /** Vehicle by ID (hash table, O(1) expected). @throws VehicleNotFoundException */
    public synchronized Vehicle findVehicle(String id) {
        return vehicles.findById(id);
    }

    /**
     * Registers a new vehicle in the hash table (RF6).
     *
     * <p>Complexity — time: average O(1); space: O(1).
     *
     * @throws DuplicateVehicleException if the ID already exists
     * @throws InvalidMapException       if the position is not a road inside the map
     */
    public synchronized Vehicle registerVehicle(String id, VehicleType type, Position position) {
        String cleanId = id == null ? "" : id.trim();
        if (cleanId.isEmpty()) {
            throw new DuplicateVehicleException("El id del vehículo no puede estar vacío");
        }
        if (vehicles.contains(cleanId)) {
            throw new DuplicateVehicleException("Ya existe un vehículo con id " + cleanId);
        }
        if (!map.canEnter(position)) {
            throw new InvalidMapException("Los vehículos deben ubicarse sobre una vía: " + position);
        }
        Vehicle vehicle = new Vehicle(cleanId, type, position);
        vehicles.add(vehicle);
        record(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, cleanId + " (" + type.getLabel() + ") registrado en " + position);
        return vehicle;
    }

    /**
     * Takes an available vehicle out of service, or puts it back (RF6).
     *
     * @throws VehicleNotAvailableException if the vehicle is busy with an incident
     */
    public synchronized void setOutOfService(String vehicleId, boolean outOfService) {
        Vehicle vehicle = vehicles.findById(vehicleId);
        VehicleStatus before = vehicle.getStatus();
        if (before == VehicleStatus.EN_ROUTE || before == VehicleStatus.ATTENDING) {
            throw new VehicleNotAvailableException("El vehículo " + vehicleId + " está ocupado ("
                    + before.getLabel() + ") y no puede cambiar de estado");
        }
        vehicle.setOutOfService(outOfService);
        if (vehicle.getStatus() != before) {
            record(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, vehicleId + ": " + before.getLabel() + " → "
                    + vehicle.getStatus().getLabel());
        }
    }

    /** Copy of the vehicle list in configuration order. Complexity — O(m). */
    public synchronized LinkedList<Vehicle> vehicleList() {
        return vehicles.all().copy();
    }

    /**
     * Proposes the best vehicle for an incident (RF7, RF28, RF33b): every available and compatible
     * vehicle goes into a priority queue ordered by its Floyd-Warshall distance to the incident
     * (closest first, then by ID), and the top is returned. The operator still confirms it.
     *
     * <p>Complexity — time: O(m log m) for m candidate vehicles once the distance matrix is up to
     * date (each distance is an O(1) lookup); space: O(m).
     *
     * @return the suggested vehicle, or {@code null} if no compatible vehicle can reach it
     */
    public synchronized Vehicle suggestVehicle(String incidentId) {
        return suggestFor(incidents.findById(incidentId));
    }

    /**
     * Assigns a vehicle to an incident after validating it (RF7): the incident must be pending,
     * and the vehicle available, compatible and connected by an open road. The route is computed
     * with Dijkstra on current traffic (RF32).
     *
     * <p>Complexity — time: O((V + E) log V) for Dijkstra, plus O(m) to compare with the other
     * candidates and O(n) to take the incident out of both queues; space: O(V + E).
     *
     * @throws IncidentNotFoundException       if the incident is not active
     * @throws VehicleNotFoundException        if the vehicle does not exist
     * @throws InvalidIncidentStateException   if the incident is already in progress
     * @throws VehicleNotAvailableException    if the vehicle is busy or out of service
     * @throws IncompatibleVehicleException    if the vehicle type cannot attend the incident
     * @throws RouteUnavailableException       if no open road connects them
     */
    public synchronized AssignmentRecord assignVehicle(String incidentId, String vehicleId) {
        Incident incident = incidents.findById(incidentId);
        Vehicle vehicle = vehicles.findById(vehicleId);
        return assign(incident, vehicle, true);
    }

    /**
     * "Attend next": extracts the most urgent pending incident from the heap in O(log n) and
     * sends the suggested vehicle.
     *
     * @throws EmptyStructureException      if there are no pending incidents
     * @throws VehicleNotAvailableException if no compatible vehicle can reach it
     */
    public synchronized AssignmentRecord attendNext() {
        if (attentionQueue.isEmpty()) {
            throw new EmptyStructureException("No hay incidentes pendientes por atender");
        }
        Incident top = attentionQueue.peekMax();
        Vehicle candidate = suggestFor(top);
        if (candidate == null) {
            throw new VehicleNotAvailableException("No hay vehículos compatibles disponibles para " + top.getId()
                    + " (" + top.getType().getLabel() + ")");
        }
        attentionQueue.extractMax();
        try {
            return assign(top, candidate, false);
        } catch (RuntimeException error) {
            attentionQueue.insert(top);
            throw error;
        }
    }

    /**
     * The vehicle reports that it is on site: it skips the rest of its route and starts attending
     * (RF8). During the simulation this happens automatically when it reaches the incident.
     *
     * @throws InvalidIncidentStateException if the incident has no vehicle on its way
     */
    public synchronized void startAttention(String incidentId) {
        Incident incident = incidents.findById(incidentId);
        if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
            throw new InvalidIncidentStateException("El incidente " + incidentId + " no tiene un vehículo en camino");
        }
        Vehicle vehicle = vehicles.findById(incident.getAssignedVehicleId());
        if (vehicle.getStatus() != VehicleStatus.EN_ROUTE) {
            throw new InvalidIncidentStateException("El vehículo " + vehicle.getId() + " ya está atendiendo");
        }
        vehicle.setPosition(incident.getPosition());
        arrive(vehicle, incident);
    }

    /**
     * Finishes the attention (RF8, RF14): marks the incident resolved, computes its points, removes
     * it from the BST and the hash index together, and frees the vehicle.
     *
     * <p>Complexity — time: average O(log n) (BST) + O(1) (hash); space: O(1).
     *
     * @throws InvalidIncidentStateException if no vehicle has been assigned or it has not arrived
     */
    public synchronized ScoreBreakdown resolveIncident(String incidentId) {
        Incident incident = incidents.findById(incidentId);
        if (incident.getStatus() == IncidentStatus.PENDING) {
            throw new InvalidIncidentStateException("El incidente " + incidentId + " todavía no tiene vehículo asignado");
        }
        Vehicle vehicle = vehicles.findById(incident.getAssignedVehicleId());
        if (vehicle.getStatus() != VehicleStatus.ATTENDING) {
            throw new InvalidIncidentStateException("El vehículo " + vehicle.getId() + " todavía no llega ("
                    + vehicle.remainingSteps() + " celdas por recorrer)");
        }
        incident.markResolved(minute);
        ScoreBreakdown points = scoring.score(incident, assignments.get(incidentId), minute);
        score += points.total();
        incidents.archive(incidentId);
        assignments.remove(incidentId);
        vehicle.release();
        actions.push(new OperatorAction(OperatorAction.Type.RESOLVE_INCIDENT,
                incidentId + " con " + vehicle.getId() + " → " + points, minute));
        record(SimulationEvent.Type.INCIDENT_RESOLVED, incidentId + " resuelto en " + incident.attentionMinutes()
                + " min: " + points);
        record(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, vehicle.getId() + ": Atendiendo → Disponible");
        return points;
    }

    /** Remaining route of a vehicle (empty when not en route). Complexity — O(L). */
    public synchronized LinkedList<Position> remainingRoute(String vehicleId) {
        return vehicles.findById(vehicleId).remainingRoute();
    }

    /** Assignment decision of an active incident, or {@code null}. Complexity — O(1). */
    public synchronized AssignmentRecord assignmentOf(String incidentId) {
        return assignments.get(incidentId);
    }

    // ================================================================= time and traffic

    /**
     * Advances the simulation clock one minute and moves every vehicle en route according to the
     * current road weights; a vehicle that reaches its incident starts attending it.
     *
     * <p>Complexity — time: O(m · s) for m vehicles advancing s cells (s is small: a minute only
     * pays for one or two segments), plus O((V + E) log V) per vehicle that must be rerouted;
     * space: O(1).
     */
    public synchronized void advanceMinute() {
        minute++;
        for (Vehicle vehicle : vehicles.all()) {
            if (vehicle.getStatus() == VehicleStatus.EN_ROUTE) {
                moveAlongRoute(vehicle);
            }
        }
    }

    /**
     * Counts moving emergency vehicles and civilian cars per district and updates the congestion
     * factor of every road segment.
     *
     * <p>Complexity — time: O(m + c + V + E); space: O(D) for D districts.
     */
    public synchronized void updateTraffic(LinkedList<Position> civilianCars) {
        int[] counts = new int[map.districtCount()];
        for (Vehicle vehicle : vehicles.all()) {
            if (vehicle.getStatus() == VehicleStatus.EN_ROUTE) {
                counts[map.districtOf(vehicle.getPosition())]++;
            }
        }
        for (Position car : civilianCars) {
            counts[map.districtOf(car)]++;
        }
        map.applyCongestion(counts);
    }

    public synchronized long currentMinute() {
        return minute;
    }

    // ================================================================= roads

    /**
     * Closes a road segment, reroutes the vehicles that were using it and reports the incidents
     * left without a reachable compatible vehicle (RF31a). The Floyd-Warshall matrix and the
     * corridors are recomputed on their next use because the topology version changed.
     *
     * @return incidents isolated by the closure
     */
    public synchronized LinkedList<Incident> blockRoad(Position a, Position b) {
        map.blockRoad(a, b);
        actions.push(new OperatorAction(OperatorAction.Type.BLOCK_ROAD, a + " - " + b, minute));
        record(SimulationEvent.Type.ROAD_BLOCKED, a + " - " + b);
        rerouteVehicles();
        return reportIsolatedIncidents();
    }

    /** Reopens a road segment. */
    public synchronized void unblockRoad(Position a, Position b) {
        map.unblockRoad(a, b);
        actions.push(new OperatorAction(OperatorAction.Type.UNBLOCK_ROAD, a + " - " + b, minute));
        record(SimulationEvent.Type.ROAD_UNBLOCKED, a + " - " + b);
        rerouteVehicles();
    }

    /**
     * Active incidents whose connected component (DFS over open roads) has no vehicle that can
     * attend them: for pending ones, no available compatible vehicle; for those in progress, their
     * own vehicle is on the other side of a closure.
     *
     * <p>Complexity — time: O(V + E) for DFS plus O(n · m) for the checks; space: O(V).
     */
    public synchronized LinkedList<Incident> isolatedIncidents() {
        DfsResult<Position> components = map.structuralAnalysis();
        LinkedList<Incident> isolated = new LinkedList<>();
        for (Incident incident : incidents.activeByPriority()) {
            boolean reachable;
            if (incident.getStatus() == IncidentStatus.PENDING) {
                reachable = vehicles.availableFor(incident.getType()).find(
                        v -> components.sameComponent(v.getPosition(), incident.getPosition())) != null;
            } else {
                Vehicle vehicle = vehicles.findById(incident.getAssignedVehicleId());
                reachable = components.sameComponent(vehicle.getPosition(), incident.getPosition());
            }
            if (!reachable) {
                isolated.addLast(incident);
            }
        }
        return isolated;
    }

    /** Copy of the blocked segments. */
    public synchronized LinkedList<Edge<Position>> blockedRoads() {
        return map.blockedRoads();
    }

    // ================================================================= analysis for the interface

    /** Risk points of the road network (DFS cycles near main roads). Complexity — O(R) copy. */
    public synchronized LinkedList<Position> riskPoints() {
        return map.riskPoints().copy();
    }

    /** Number of connected components of the open road network (DFS). Complexity — O(V + E). */
    public synchronized int connectedComponents() {
        return map.structuralAnalysis().componentCount();
    }

    /** Active incidents of one severity, most urgent first (BST query). Complexity — O(n). */
    public synchronized LinkedList<Incident> activeIncidentsWithSeverity(Severity severity) {
        return incidents.activeWithSeverity(severity);
    }

    /** BFS coverage of the monitoring center per zone. */
    public synchronized LinkedList<ZoneCoverage> coverage() {
        return map.coverage();
    }

    /** Estimated response time to a zone, O(1) per query (Floyd-Warshall precomputed). */
    public synchronized double responseTime(ZoneType zone) {
        return routes().responseTime(zone);
    }

    /**
     * Structural distance between two cells from the Floyd-Warshall matrix.
     *
     * <p>Complexity — O(1) once the matrix is up to date; Θ(V³) if the topology changed.
     */
    public synchronized double structuralDistance(Position a, Position b) {
        return routes().structuralDistance(a, b);
    }

    /** Duration in ms of the last Floyd-Warshall recomputation. */
    public synchronized long lastFloydWarshallMillis() {
        routes().distanceMatrix();
        return routes().getLastFloydWarshallMillis();
    }

    /** Emergency corridors (MST) computed with Prim. */
    public synchronized MstResult<Position> primCorridors() {
        return routes().primCorridors();
    }

    /** Emergency corridors (MST) computed with Kruskal. */
    public synchronized MstResult<Position> kruskalCorridors() {
        return routes().kruskalCorridors();
    }

    /** Current weight of a road segment (with congestion). */
    public synchronized double currentWeight(Position a, Position b) {
        return map.currentWeight(a, b);
    }

    /** Snapshot of the indicators (RF13). Complexity — O(n + m). */
    public synchronized Indicators indicators() {
        return new Indicators(minute, score, incidents.activeCount(),
                incidents.activeCount(IncidentType.ACCIDENT), incidents.activeCount(IncidentType.ROBBERY),
                incidents.activeCount(IncidentType.FIRE), dispatchQueue.size(), vehicles.availableCount(),
                vehicles.size(), incidents.resolvedHistory().size());
    }

    public synchronized int getScore() {
        return score;
    }

    /** The map. Its grid never changes; road state should be read through this class. */
    public CityMap getMap() {
        return map;
    }

    // ================================================================= operator

    /** Moves the operator's cursor (RF21). @throws InvalidMovementException */
    public synchronized Movement moveOperator(Direction direction) {
        return operator.move(direction, minute);
    }

    /** Undoes the last movement (U key). @throws EmptyStructureException if there is none */
    public synchronized Movement undoOperatorMovement() {
        return operator.undo();
    }

    public synchronized Position operatorPosition() {
        return operator.getPosition();
    }

    public synchronized int operatorHistorySize() {
        return operator.historySize();
    }

    /**
     * Last administrative action (top of the stack, O(1)).
     *
     * @throws EmptyStructureException if no action has been performed yet
     */
    public synchronized OperatorAction lastAction() {
        return actions.peek();
    }

    public synchronized boolean hasActions() {
        return !actions.isEmpty();
    }

    /** Administrative actions from the most recent to the oldest. Complexity — O(k). */
    public synchronized LinkedList<OperatorAction> actionHistory() {
        LinkedList<OperatorAction> copy = new LinkedList<>();
        for (OperatorAction action : actions) {
            copy.addLast(action);
        }
        return copy;
    }

    // ================================================================= events

    /**
     * Removes and returns every pending event, oldest first (consumer side of RF24).
     *
     * <p>Complexity — time: O(k) for k events; space: O(k).
     */
    public synchronized LinkedList<SimulationEvent> pollEvents() {
        LinkedList<SimulationEvent> drained = new LinkedList<>();
        while (!events.isEmpty()) {
            drained.addLast(events.dequeue());
        }
        return drained;
    }

    /** The most recent events (at most 300), oldest first, kept across save and load. */
    public synchronized LinkedList<SimulationEvent> eventHistory() {
        return history.copy();
    }

    /**
     * Checks the invariants that tie the structures together: the BST and the hash index hold the
     * same incidents, and the dispatch queue and the attention heap hold exactly the pending ones.
     * Used by the concurrency tests.
     *
     * <p>Complexity — time: O(n²) in the worst case (queue membership checks); space: O(n).
     */
    public synchronized boolean isConsistent() {
        if (!incidents.isConsistent()) {
            return false;
        }
        int pending = 0;
        for (Incident incident : incidents.activeByPriority()) {
            if (incident.getStatus() == IncidentStatus.PENDING) {
                pending++;
                if (!dispatchQueue.contains(incident) || !attentionQueue.contains(incident)) {
                    return false;
                }
            }
        }
        return dispatchQueue.size() == pending && attentionQueue.size() == pending;
    }

    /** Lets other model components (the simulation) publish an event. */
    public synchronized void publish(SimulationEvent.Type type, String message) {
        record(type, message);
    }

    // ================================================================= internals

    private RouteService routes() {
        if (routes == null) {
            routes = new RouteService(map);
        }
        return routes;
    }

    private Vehicle suggestFor(Incident incident) {
        AllPairsResult<Position> matrix = routes().distanceMatrix();
        PriorityQueue<Vehicle> ranking = new PriorityQueue<>((a, b) -> {
            int byDistance = Double.compare(matrix.distance(b.getPosition(), incident.getPosition()),
                    matrix.distance(a.getPosition(), incident.getPosition()));
            return byDistance != 0 ? byDistance : b.getId().compareTo(a.getId());
        });
        for (Vehicle vehicle : vehicles.availableFor(incident.getType())) {
            if (matrix.isReachable(vehicle.getPosition(), incident.getPosition())) {
                ranking.insert(vehicle);
            }
        }
        return ranking.isEmpty() ? null : ranking.peekMax();
    }

    private AssignmentRecord assign(Incident incident, Vehicle vehicle, boolean removeFromAttentionQueue) {
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentStateException("El incidente " + incident.getId() + " está resuelto y no puede recibir vehículos");
        }
        if (incident.getStatus() == IncidentStatus.IN_PROGRESS) {
            throw new InvalidIncidentStateException("El incidente " + incident.getId() + " ya está siendo atendido por "
                    + incident.getAssignedVehicleId());
        }
        if (!vehicle.isAvailable()) {
            throw new VehicleNotAvailableException("El vehículo " + vehicle.getId() + " no está disponible ("
                    + vehicle.getStatus().getLabel() + ")");
        }
        if (!vehicle.canAttend(incident.getType())) {
            throw new IncompatibleVehicleException("Un(a) " + vehicle.getType().getLabel().toLowerCase(Locale.ROOT)
                    + " no puede atender un " + incident.getType().getLabel().toLowerCase(Locale.ROOT));
        }
        ShortestPathResult<Position> fromIncident = routes().routesFrom(incident.getPosition());
        double chosenCost = fromIncident.distanceTo(vehicle.getPosition());
        if (chosenCost == Double.POSITIVE_INFINITY) {
            throw new RouteUnavailableException("No hay una vía abierta desde " + vehicle.getId() + " hasta " + incident.getId());
        }
        AllPairsResult<Position> matrix = routes().distanceMatrix();
        double chosenStructural = matrix.distance(vehicle.getPosition(), incident.getPosition());
        double bestCost = chosenCost;
        double bestStructural = chosenStructural;
        for (Vehicle candidate : vehicles.availableFor(incident.getType())) {
            bestCost = Math.min(bestCost, fromIncident.distanceTo(candidate.getPosition()));
            bestStructural = Math.min(bestStructural, matrix.distance(candidate.getPosition(), incident.getPosition()));
        }
        LinkedList<Position> path = reversed(fromIncident.pathTo(vehicle.getPosition()));

        incident.markInProgress(vehicle.getId(), minute);
        vehicle.dispatch(incident.getId(), path);
        dispatchQueue.remove(incident);
        if (removeFromAttentionQueue) {
            attentionQueue.remove(incident);
        }
        AssignmentRecord decision = new AssignmentRecord(incident.getId(), vehicle.getId(), minute, chosenCost,
                bestCost, chosenStructural, bestStructural);
        assignments.put(incident.getId(), decision);
        actions.push(new OperatorAction(OperatorAction.Type.ASSIGN_VEHICLE, vehicle.getId() + " → " + incident.getId()
                + String.format(Locale.ROOT, " (ruta %.1f, ~%d min)", chosenCost, decision.estimatedMinutes()), minute));
        record(SimulationEvent.Type.VEHICLE_ASSIGNED, vehicle.getId() + " va hacia " + incident.getId()
                + String.format(Locale.ROOT, " — tiempo estimado %d min", decision.estimatedMinutes()));
        record(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, vehicle.getId() + ": Disponible → En ruta");
        if (vehicle.remainingSteps() == 0) {
            arrive(vehicle, incident);
        }
        return decision;
    }

    private void moveAlongRoute(Vehicle vehicle) {
        vehicle.addTravelBudget(1.0);
        while (vehicle.nextStep() != null) {
            Position next = vehicle.nextStep();
            double weight = map.currentWeight(vehicle.getPosition(), next);
            if (weight == Double.POSITIVE_INFINITY) {
                if (!reroute(vehicle)) {
                    vehicle.spendTravelBudget(vehicle.getTravelBudget());
                    return;
                }
                continue;
            }
            if (vehicle.getTravelBudget() + 1e-9 < weight) {
                return;
            }
            vehicle.spendTravelBudget(weight);
            vehicle.advance();
        }
        arrive(vehicle, incidents.findById(vehicle.getAssignedIncidentId()));
    }

    private boolean reroute(Vehicle vehicle) {
        Incident incident = incidents.findById(vehicle.getAssignedIncidentId());
        ShortestPathResult<Position> paths = routes().routesFrom(vehicle.getPosition());
        if (!paths.isReachable(incident.getPosition())) {
            return false;
        }
        vehicle.dispatch(incident.getId(), paths.pathTo(incident.getPosition()));
        return true;
    }

    private void rerouteVehicles() {
        for (Vehicle vehicle : vehicles.all()) {
            if (vehicle.getStatus() == VehicleStatus.EN_ROUTE && !reroute(vehicle)) {
                record(SimulationEvent.Type.ISOLATED_INCIDENT, vehicle.getId() + " no tiene ruta abierta hacia "
                        + vehicle.getAssignedIncidentId());
            }
        }
    }

    private LinkedList<Incident> reportIsolatedIncidents() {
        LinkedList<Incident> isolated = isolatedIncidents();
        for (Incident incident : isolated) {
            record(SimulationEvent.Type.ISOLATED_INCIDENT, incident.getId() + " quedó sin vehículos que puedan llegar");
        }
        return isolated;
    }

    private void arrive(Vehicle vehicle, Incident incident) {
        vehicle.startAttending();
        record(SimulationEvent.Type.VEHICLE_ARRIVED, vehicle.getId() + " llegó a " + incident.getId());
        record(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, vehicle.getId() + ": En ruta → Atendiendo");
    }

    private void record(SimulationEvent.Type type, String message) {
        SimulationEvent event = new SimulationEvent(type, message, minute);
        events.enqueue(event);
        history.addLast(event);
        if (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    private static LinkedList<Position> reversed(LinkedList<Position> path) {
        LinkedList<Position> result = new LinkedList<>();
        for (Position p : path) {
            result.addFirst(p);
        }
        return result;
    }
}
