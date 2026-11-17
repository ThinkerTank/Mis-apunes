package co.edu.icesi.sgmms.model.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.MapSettings;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import org.junit.jupiter.api.Test;

class AccidentDetectorTest {

    private static LinkedList<CarMove> moves(CarMove... list) {
        LinkedList<CarMove> result = new LinkedList<>();
        for (CarMove move : list) {
            result.addLast(move);
        }
        return result;
    }

    private static LinkedList<AccidentReport> only(LinkedList<AccidentReport> reports, AccidentCriterion criterion) {
        return reports.filter(r -> r.getCriterion() == criterion);
    }

    @Test
    void twoVehiclesEnteringTheSameCellCollide() {
        CityMap map = SimFixtures.map();
        Position target = new Position(4, 3);
        AccidentDetector detector = new AccidentDetector(5);

        LinkedList<AccidentReport> reports = detector.detect(map, moves(
                new CarMove("CIV-01", new Position(3, 3), target),
                new CarMove("CIV-02", new Position(5, 3), target)), new HashTable<>(), 10);

        LinkedList<AccidentReport> collisions = only(reports, AccidentCriterion.COLLISION);
        assertEquals(1, collisions.size());
        assertEquals(target, collisions.getFirst().getPosition());
        assertEquals(2, collisions.getFirst().getInvolved().size());
    }

    @Test
    void aDistrictOverItsCapacityProducesACongestionAccident() {
        MapSettings settings = new MapSettings();
        settings.setZoneCapacity(2);
        CityMap map = SimFixtures.map(settings);
        AccidentDetector detector = new AccidentDetector(5);

        LinkedList<AccidentReport> reports = detector.detect(map, moves(
                new CarMove("CIV-01", new Position(0, 0), new Position(1, 0)),
                new CarMove("CIV-02", new Position(3, 0), new Position(2, 0)),
                new CarMove("CIV-03", new Position(0, 3), new Position(0, 2))), new HashTable<>(), 10);

        LinkedList<AccidentReport> congestion = only(reports, AccidentCriterion.CRITICAL_CONGESTION);
        assertEquals(1, congestion.size());
        assertEquals(3, congestion.getFirst().getInvolved().size());
    }

    @Test
    void vehiclesFromDifferentDirectionsConvergingOnARiskPointCrash() {
        CityMap map = SimFixtures.map();
        Position risk = map.riskPoints().getFirst();
        LinkedList<Position> neighbors = map.openNeighbors(risk);
        Position first = neighbors.get(0);
        Position second = neighbors.get(1);
        AccidentDetector detector = new AccidentDetector(5);

        LinkedList<AccidentReport> reports = detector.detect(map, moves(
                new CarMove("CIV-01", new Position(0, 6), first),
                new CarMove("CIV-02", new Position(9, 6), second)), new HashTable<>(), 10);

        LinkedList<AccidentReport> convergence = only(reports, AccidentCriterion.RISK_POINT_CONVERGENCE);
        assertEquals(1, convergence.size());
        assertEquals(risk, convergence.getFirst().getPosition());
    }

    @Test
    void theSameCriterionWaitsForItsCooldownInTheSamePlace() {
        CityMap map = SimFixtures.map();
        Position target = new Position(4, 3);
        AccidentDetector detector = new AccidentDetector(5);
        CarMove a = new CarMove("CIV-01", new Position(3, 3), target);
        CarMove b = new CarMove("CIV-02", new Position(5, 3), target);

        assertEquals(1, only(detector.detect(map, moves(a, b), new HashTable<>(), 10), AccidentCriterion.COLLISION).size());
        assertEquals(0, only(detector.detect(map, moves(a, b), new HashTable<>(), 12), AccidentCriterion.COLLISION).size());
        assertEquals(1, only(detector.detect(map, moves(a, b), new HashTable<>(), 15), AccidentCriterion.COLLISION).size());
    }

    @Test
    void aVehicleThatAlreadyCrashedIsNotCountedTwiceInTheSameMinute() {
        CityMap map = SimFixtures.map();
        Position risk = map.riskPoints().getFirst();
        LinkedList<AccidentReport> reports = new AccidentDetector(5).detect(map, moves(
                new CarMove("CIV-01", new Position(0, 6), risk),
                new CarMove("CIV-02", new Position(9, 6), risk)), new HashTable<>(), 10);

        assertEquals(1, reports.size());
        assertEquals(AccidentCriterion.COLLISION, reports.getFirst().getCriterion());
    }

    @Test
    void noAccidentIsRaisedWhereAnIncidentIsAlreadyActive() {
        CityMap map = SimFixtures.map();
        Position target = new Position(4, 3);
        HashTable<Position, Boolean> busy = new HashTable<>();
        busy.put(target, Boolean.TRUE);

        LinkedList<AccidentReport> reports = new AccidentDetector(5).detect(map, moves(
                new CarMove("CIV-01", new Position(3, 3), target),
                new CarMove("CIV-02", new Position(5, 3), target)), busy, 10);

        assertTrue(reports.find(r -> r.getCriterion() == AccidentCriterion.COLLISION) == null);
    }
}
