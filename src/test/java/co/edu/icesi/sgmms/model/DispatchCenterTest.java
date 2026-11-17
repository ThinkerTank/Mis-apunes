package co.edu.icesi.sgmms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import co.edu.icesi.sgmms.model.exceptions.IncidentNotFoundException;
import co.edu.icesi.sgmms.model.exceptions.IncompatibleVehicleException;
import co.edu.icesi.sgmms.model.exceptions.InvalidIncidentStateException;
import co.edu.icesi.sgmms.model.exceptions.VehicleNotAvailableException;
import co.edu.icesi.sgmms.model.exceptions.VehicleNotFoundException;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatchCenterTest {

    private static final Position ACCIDENT_SPOT = new Position(2, 3);

    private DispatchCenter center;

    @BeforeEach
    void setUp() {
        VehicleRepository vehicles = new VehicleRepository();
        vehicles.add(new Vehicle("P-1", VehicleType.PATROL, new Position(0, 0)));
        vehicles.add(new Vehicle("A-1", VehicleType.AMBULANCE, new Position(7, 5)));
        vehicles.add(new Vehicle("A-2", VehicleType.AMBULANCE, new Position(1, 3)));
        vehicles.add(new Vehicle("B-1", VehicleType.FIRE_TRUCK, new Position(7, 0)));
        center = new DispatchCenter(TestCity.map(), vehicles);
    }

    private void driveUntilArrival(String vehicleId) {
        for (int i = 0; i < 60 && center.findVehicle(vehicleId).getStatus() == VehicleStatus.EN_ROUTE; i++) {
            center.advanceMinute();
        }
    }

    @Test
    void newIncidentEntersQueueHeapTreeAndIndexAndOperatorMoveIsStacked() {
        Incident incident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");
        center.moveOperator(Direction.RIGHT);

        assertEquals(incident, center.nextInDispatchQueue());
        assertEquals(incident, center.mostUrgentPending());
        assertEquals(incident, center.mostUrgentActive());
        assertEquals(incident, center.findIncident(incident.getId()));
        assertEquals(1, center.operatorHistorySize());
        assertEquals(new Position(1, 0), center.operatorPosition());
        assertEquals(SimulationEvent.Type.INCIDENT_CREATED, center.pollEvents().getFirst().getType());
    }

    @Test
    void invalidAssignmentsAreRejectedAndEarnNoPoints() {
        Incident robbery = center.reportIncident(IncidentType.ROBBERY, new Position(3, 1), Severity.MEDIUM, "robo");
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");

        assertThrows(IncompatibleVehicleException.class, () -> center.assignVehicle(robbery.getId(), "B-1"));
        assertThrows(VehicleNotFoundException.class, () -> center.assignVehicle(robbery.getId(), "X-9"));
        assertThrows(IncidentNotFoundException.class, () -> center.assignVehicle("INC-9999", "P-1"));

        center.assignVehicle(accident.getId(), "A-2");
        assertThrows(InvalidIncidentStateException.class, () -> center.assignVehicle(accident.getId(), "A-1"));
        Incident second = center.reportIncident(IncidentType.ACCIDENT, new Position(6, 3), Severity.LOW, "otro");
        assertThrows(VehicleNotAvailableException.class, () -> center.assignVehicle(second.getId(), "A-2"));

        assertEquals(0, center.getScore());
    }

    @Test
    void suggestionIsTheClosestCompatibleAvailableVehicle() {
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");

        assertEquals("A-2", center.suggestVehicle(accident.getId()).getId());
        center.assignVehicle(accident.getId(), "A-2");
        Incident other = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.LOW, "otro");
        assertEquals("P-1", center.suggestVehicle(other.getId()).getId());
    }

    @Test
    void attendNextTakesTheMostUrgentAndBreaksTiesByAge() {
        Incident oldMedium = center.reportIncident(IncidentType.ROBBERY, new Position(3, 1), Severity.MEDIUM, "a");
        center.advanceMinute();
        center.reportIncident(IncidentType.ROBBERY, new Position(4, 4), Severity.MEDIUM, "b");
        center.reportIncident(IncidentType.ROBBERY, new Position(6, 1), Severity.LOW, "c");

        AssignmentRecord decision = center.attendNext();

        assertEquals(oldMedium.getId(), decision.getIncidentId());
        assertEquals("P-1", decision.getVehicleId());
        assertEquals(2, center.dispatchQueueSnapshot().size());
        assertFalse(center.dispatchQueueSnapshot().contains(oldMedium));
    }

    @Test
    void attendNextWithoutCompatibleVehicleKeepsTheIncidentPending() {
        center.findVehicle("B-1").setOutOfService(true);
        Incident fire = center.reportIncident(IncidentType.FIRE, new Position(4, 1), Severity.HIGH, "incendio");

        assertThrows(VehicleNotAvailableException.class, center::attendNext);
        assertEquals(fire, center.mostUrgentPending());
        assertEquals(IncidentStatus.PENDING, center.findIncident(fire.getId()).getStatus());
    }

    @Test
    void vehicleFollowsTheDijkstraRouteArrivesAndResolutionScores() {
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");
        AssignmentRecord decision = center.assignVehicle(accident.getId(), "A-2");
        LinkedList<Position> route = center.remainingRoute("A-2");

        assertEquals(ACCIDENT_SPOT, route.getLast());
        assertTrue(decision.choseCheapestRoute());
        assertThrows(InvalidIncidentStateException.class, () -> center.resolveIncident(accident.getId()));

        driveUntilArrival("A-2");
        assertEquals(VehicleStatus.ATTENDING, center.findVehicle("A-2").getStatus());
        ScoreBreakdown points = center.resolveIncident(accident.getId());

        assertEquals(100 + 20 + 10, points.total());
        assertEquals(130, center.getScore());
        assertTrue(center.findVehicle("A-2").isAvailable());
        assertThrows(IncidentNotFoundException.class, () -> center.findIncident(accident.getId()));
        assertNull(center.mostUrgentActive());
    }

    @Test
    void sendingAFarVehicleLosesTheRoutingBonusAndIsPenalized() {
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");
        AssignmentRecord decision = center.assignVehicle(accident.getId(), "A-1");

        assertFalse(decision.choseCheapestRoute());
        assertTrue(decision.exceedsOptimum(ScoreCalculator.PENALTY_FACTOR));
        driveUntilArrival("A-1");
        ScoreBreakdown points = center.resolveIncident(accident.getId());

        assertEquals(0, points.getRoutingBonus());
        assertEquals(ScoreCalculator.ROUTING_PENALTY, points.getRoutingPenalty());
    }

    @Test
    void lastActionIsTheTopOfTheActionStack() {
        assertThrows(EmptyStructureException.class, center::lastAction);
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");
        center.assignVehicle(accident.getId(), "A-2");
        assertEquals(OperatorAction.Type.ASSIGN_VEHICLE, center.lastAction().getType());

        center.startAttention(accident.getId());
        center.resolveIncident(accident.getId());
        assertEquals(OperatorAction.Type.RESOLVE_INCIDENT, center.lastAction().getType());
        assertEquals(2, center.actionHistory().size());
    }

    @Test
    void eventsAreConsumedInTheOrderTheyHappened() {
        Incident accident = center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "choque");
        center.assignVehicle(accident.getId(), "A-2");

        LinkedList<SimulationEvent> events = center.pollEvents();
        assertEquals(SimulationEvent.Type.INCIDENT_CREATED, events.get(0).getType());
        assertEquals(SimulationEvent.Type.VEHICLE_ASSIGNED, events.get(1).getType());
        assertEquals(SimulationEvent.Type.VEHICLE_STATUS_CHANGED, events.get(2).getType());
        assertTrue(center.pollEvents().isEmpty());
    }

    @Test
    void indicatorsCountActiveIncidentsByTypeAndAvailableVehicles() {
        center.reportIncident(IncidentType.ACCIDENT, ACCIDENT_SPOT, Severity.HIGH, "a");
        center.reportIncident(IncidentType.FIRE, new Position(4, 1), Severity.LOW, "b");
        Incident robbery = center.reportIncident(IncidentType.ROBBERY, new Position(3, 1), Severity.LOW, "c");
        center.assignVehicle(robbery.getId(), "P-1");

        Indicators now = center.indicators();
        assertEquals(3, now.getActiveIncidents());
        assertEquals(1, now.getAccidents());
        assertEquals(1, now.getFires());
        assertEquals(1, now.getRobberies());
        assertEquals(2, now.getPendingIncidents());
        assertEquals(3, now.getAvailableVehicles());
    }

    @Test
    void floydWarshallIsRecomputedWhenARoadIsBlocked() {
        Position a = new Position(0, 0);
        Position b = new Position(1, 0);
        assertEquals(1.0, center.structuralDistance(a, b), 1e-9);

        center.blockRoad(a, b);

        // detour (0,0) → (0,1) → (1,1) → (1,0): 1.0 + 1.5 + 1.5
        assertEquals(4.0, center.structuralDistance(a, b), 1e-9);
        assertEquals(OperatorAction.Type.BLOCK_ROAD, center.lastAction().getType());
        center.unblockRoad(a, b);
        assertEquals(1.0, center.structuralDistance(a, b), 1e-9);
    }

    @Test
    void blockingTheOnlyRoadIsolatesTheIncidentOnTheOtherSide() {
        VehicleRepository vehicles = new VehicleRepository();
        vehicles.add(new Vehicle("A-1", VehicleType.AMBULANCE, new Position(0, 0)));
        DispatchCenter line = new DispatchCenter(
                new CityMap(new int[][] {{2, 4, 4, 2}}, new Position(0, 0), new MapSettings()), vehicles);
        Incident far = line.reportIncident(IncidentType.ACCIDENT, new Position(3, 0), Severity.HIGH, "lejos");

        LinkedList<Incident> isolated = line.blockRoad(new Position(1, 0), new Position(2, 0));

        assertEquals(1, isolated.size());
        assertEquals(far, isolated.getFirst());
        assertNull(line.suggestVehicle(far.getId()));
    }

    @Test
    void primAndKruskalCorridorsHaveTheSameCostOnTheCity() {
        assertEquals(center.primCorridors().getTotalWeight(), center.kruskalCorridors().getTotalWeight(), 1e-9);
        assertTrue(center.kruskalCorridors().spansAllVertices());
    }
}
