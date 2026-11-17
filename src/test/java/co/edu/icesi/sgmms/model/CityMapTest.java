package co.edu.icesi.sgmms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.InvalidMapException;
import co.edu.icesi.sgmms.model.structures.DfsResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class CityMapTest {

    private static final double EPS = 1e-9;

    @Test
    void everyTraversableCellBecomesAVertexInTheThreeGraphs() {
        CityMap map = TestCity.map();
        int traversable = 8 * 6 - 2;

        assertEquals(traversable, map.baseGraph().vertexCount());
        assertEquals(traversable, map.trafficGraph().vertexCount());
        assertEquals(traversable, map.matrixGraph().vertexCount());
        assertEquals(map.baseGraph().edgeCount(), map.matrixGraph().edgeCount());
    }

    @Test
    void baseWeightDependsOnTheZonesOfBothCells() {
        CityMap map = TestCity.map();

        assertEquals(1.0, map.baseWeight(new Position(0, 0), new Position(1, 0)), EPS);
        assertEquals(1.5, map.baseWeight(new Position(0, 1), new Position(1, 1)), EPS);
        assertEquals(2.5, map.baseWeight(new Position(4, 1), new Position(4, 2)), EPS);
    }

    @Test
    void invalidGridsAreRejected() {
        assertThrows(InvalidMapException.class,
                () -> new CityMap(new int[][] {{4, 9}}, new Position(0, 0), new MapSettings()));
        assertThrows(InvalidMapException.class,
                () -> new CityMap(new int[][] {{4, 4}, {4}}, new Position(0, 0), new MapSettings()));
        assertThrows(InvalidMapException.class,
                () -> new CityMap(new int[][] {{1, 4}}, new Position(0, 0), new MapSettings()));
    }

    @Test
    void bfsRejectsAMapWithAnEnclosedZone() {
        int[][] enclosed = {
            {4, 4, 4, 4},
            {4, 1, 1, 4},
            {1, 2, 1, 4},
            {4, 1, 4, 4},
        };

        InvalidMapException error = assertThrows(InvalidMapException.class,
                () -> new CityMap(enclosed, new Position(0, 0), new MapSettings()));
        assertTrue(error.getMessage().contains("(1,2)"));
    }

    @Test
    void coverageReportsHopsFromTheMonitoringCenter() {
        CityMap map = TestCity.map();

        for (ZoneCoverage zone : map.coverage()) {
            assertTrue(zone.getCells() > 0);
            assertTrue(zone.getMaxHops() >= zone.getAverageHops());
        }
    }

    @Test
    void blockingAndUnblockingARoadUpdatesEveryGraph() {
        CityMap map = TestCity.map();
        Position a = new Position(0, 0);
        Position b = new Position(1, 0);
        int version = map.topologyVersion();

        map.blockRoad(a, b);
        assertTrue(map.isBlocked(b, a));
        assertFalse(map.baseGraph().hasEdge(a, b));
        assertFalse(map.matrixGraph().hasEdge(a, b));
        assertEquals(Double.POSITIVE_INFINITY, map.currentWeight(a, b));
        assertEquals(version + 1, map.topologyVersion());

        map.unblockRoad(b, a);
        assertTrue(map.trafficGraph().hasEdge(a, b));
        assertEquals(version + 2, map.topologyVersion());
        assertThrows(InvalidMapException.class, () -> map.unblockRoad(a, b));
        assertThrows(InvalidMapException.class, () -> map.blockRoad(a, new Position(3, 3)));
    }

    @Test
    void dfsFindsTwoComponentsWhenABlockedRoadSplitsTheCity() {
        CityMap line = new CityMap(new int[][] {{2, 4, 4, 2}}, new Position(0, 0), new MapSettings());
        line.blockRoad(new Position(1, 0), new Position(2, 0));

        DfsResult<Position> analysis = line.structuralAnalysis();
        assertEquals(2, analysis.componentCount());
        assertFalse(analysis.sameComponent(new Position(0, 0), new Position(3, 0)));
    }

    @Test
    void congestionMultipliesTheWeightOfSegmentsInBusyDistricts() {
        CityMap map = TestCity.map();
        Position a = new Position(0, 0);
        Position b = new Position(1, 0);
        int[] counts = new int[map.districtCount()];
        counts[map.districtOf(a)] = 2;

        map.applyCongestion(counts);

        assertEquals(1.0 * (1 + 0.25 * 2), map.currentWeight(a, b), EPS);
        assertEquals(1.0, map.baseGraph().getWeight(a, b), EPS);
    }

    @Test
    void riskPointsAreIntersectionsOnCyclesNearMainRoads() {
        CityMap map = TestCity.map();

        assertFalse(map.riskPoints().isEmpty());
        for (Position p : map.riskPoints()) {
            assertTrue(map.baseGraph().getAdjacent(p).size() >= 3);
            assertTrue(map.isRiskPoint(p));
        }
    }

    @Test
    void serializationRebuildsTheGraphsAndKeepsBlockedRoads() throws Exception {
        CityMap map = TestCity.map();
        map.blockRoad(new Position(0, 0), new Position(1, 0));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(map);
        }
        CityMap copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (CityMap) in.readObject();
        }

        assertEquals(map.baseGraph().vertexCount(), copy.baseGraph().vertexCount());
        assertTrue(copy.isBlocked(new Position(0, 0), new Position(1, 0)));
        assertFalse(copy.trafficGraph().hasEdge(new Position(0, 0), new Position(1, 0)));
    }
}
