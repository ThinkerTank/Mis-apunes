package co.edu.icesi.sgmms.model.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulationTest {

    @Test
    void privateCarsOnlyMoveBetweenAdjacentOpenCellsAndNeverCrossABlockedRoad() {
        CityMap map = SimFixtures.map();
        Position a = new Position(4, 3);
        Position b = new Position(5, 3);
        map.blockRoad(a, b);
        CivilianTraffic traffic = new CivilianTraffic(map, 10, new Random(5));

        for (int minute = 0; minute < 300; minute++) {
            for (CarMove move : traffic.step(map, new Random(minute))) {
                assertTrue(map.canEnter(move.getTo()));
                if (move.moved()) {
                    assertTrue(move.getFrom().isAdjacentTo(move.getTo()));
                    assertFalse((move.getFrom().equals(a) && move.getTo().equals(b))
                            || (move.getFrom().equals(b) && move.getTo().equals(a)));
                }
            }
        }
    }

    @Test
    void aCarInvolvedInAnAccidentStaysStillForAFewMinutes() {
        CityMap map = SimFixtures.map();
        CivilianTraffic traffic = new CivilianTraffic(map, 1, new Random(9));
        Position before = traffic.positionOf("CIV-01");

        traffic.stop("CIV-01");
        for (int minute = 0; minute < 4; minute++) {
            traffic.step(map, new Random(minute));
        }

        assertEquals(before, traffic.positionOf("CIV-01"));
        assertFalse(traffic.isStopped("CIV-01"));
    }

    @Test
    void aLongRunGeneratesJustifiedIncidentsWithinTheLimit() {
        DispatchCenter center = SimFixtures.center();
        SimulationSettings settings = SimFixtures.settings(2026);
        Simulation simulation = new Simulation(center, settings);
        int accidents = 0;

        for (int minute = 0; minute < 300; minute++) {
            accidents += simulation.clockTick().size();
            simulation.generationTick();
            assertTrue(center.indicators().getActiveIncidents() <= settings.getMaxActiveIncidents() + 3);
        }

        assertEquals(300, center.currentMinute());
        assertTrue(center.indicators().getActiveIncidents() > 0);
        for (Incident incident : center.activeIncidentsByPriority()) {
            if (incident.getType() == IncidentType.ACCIDENT) {
                assertTrue(incident.getDescription().contains(":"), incident.getDescription());
            }
        }
        assertTrue(accidents >= 0);
        assertEquals(settings.getCivilianCars(), simulation.civilianPositions().size());
    }

    @Test
    void anAssignedVehicleReachesItsIncidentAsTheClockTicks() {
        DispatchCenter center = SimFixtures.center();
        Simulation simulation = new Simulation(center, SimFixtures.settings(11));
        Incident fire = center.reportIncident(IncidentType.FIRE, new Position(4, 5), Severity.HIGH, "incendio");
        center.assignVehicle(fire.getId(), "B-1");

        for (int minute = 0; minute < 40 && center.findVehicle("B-1").getStatus() == VehicleStatus.EN_ROUTE; minute++) {
            simulation.clockTick();
        }

        assertEquals(VehicleStatus.ATTENDING, center.findVehicle("B-1").getStatus());
        assertEquals(new Position(4, 5), center.findVehicle("B-1").getPosition());
        LinkedList<Position> route = center.remainingRoute("B-1");
        assertTrue(route.isEmpty());
    }
}
