package co.edu.icesi.sgmms.model.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.ZoneType;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.util.Random;
import org.junit.jupiter.api.Test;

class IncidentGeneratorTest {

    @Test
    void withCertainProbabilitiesOneRobberyAndOneFireAppearEachMinute() {
        DispatchCenter center = SimFixtures.center();
        SimulationSettings settings = SimFixtures.settings(1);
        settings.setRobberyProbability(1.0);
        settings.setFireProbability(1.0);

        LinkedList<Incident> created = new IncidentGenerator().tick(center, settings, new Random(1), new HashTable<>());

        assertEquals(2, created.size());
        assertEquals(IncidentType.ROBBERY, created.get(0).getType());
        assertEquals(IncidentType.FIRE, created.get(1).getType());
        assertNotEquals(created.get(0).getPosition(), created.get(1).getPosition());
    }

    @Test
    void robberiesConcentrateInCommercialZonesAndAvoidMainRoads() {
        CityMap map = SimFixtures.map();
        IncidentGenerator generator = new IncidentGenerator();
        Random random = new Random(42);
        int commercial = 0;
        int mainRoad = 0;
        for (int i = 0; i < 2000; i++) {
            Position p = generator.pickPosition(map, IncidentGenerator.robberyWeights(), random, new HashTable<>());
            ZoneType zone = map.zoneAt(p);
            if (zone == ZoneType.COMMERCIAL) {
                commercial++;
            } else if (zone == ZoneType.MAIN_ROAD) {
                mainRoad++;
            }
        }
        assertTrue(commercial > 4 * mainRoad, "commercial=" + commercial + " main=" + mainRoad);
    }

    @Test
    void firesNeverStartOnTheRoadwayOrObstacles() {
        CityMap map = SimFixtures.map();
        IncidentGenerator generator = new IncidentGenerator();
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            ZoneType zone = map.zoneAt(generator.pickPosition(map, IncidentGenerator.fireWeights(), random,
                    new HashTable<>()));
            assertNotEquals(ZoneType.MAIN_ROAD, zone);
            assertNotEquals(ZoneType.OBSTACLE, zone);
        }
    }

    @Test
    void generationStopsAtTheMaximumNumberOfActiveIncidents() {
        DispatchCenter center = SimFixtures.center();
        SimulationSettings settings = SimFixtures.settings(3);
        settings.setRobberyProbability(1.0);
        settings.setFireProbability(1.0);
        settings.setMaxActiveIncidents(3);
        IncidentGenerator generator = new IncidentGenerator();
        Random random = new Random(3);

        for (int minute = 0; minute < 20; minute++) {
            generator.tick(center, settings, random, new HashTable<>());
        }

        assertTrue(center.indicators().getActiveIncidents() <= 4);
        assertTrue(center.indicators().getActiveIncidents() >= 3);
    }
}
