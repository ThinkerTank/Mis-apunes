package co.edu.icesi.sgmms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import co.edu.icesi.sgmms.model.exceptions.InvalidMovementException;
import co.edu.icesi.sgmms.model.exceptions.VehicleNotFoundException;
import org.junit.jupiter.api.Test;

class VehicleAndNavigatorTest {

    @Test
    void compatibilityRulesMatchTheStatement() {
        assertTrue(VehicleType.PATROL.canAttend(IncidentType.ROBBERY));
        assertTrue(VehicleType.PATROL.canAttend(IncidentType.ACCIDENT));
        assertFalse(VehicleType.PATROL.canAttend(IncidentType.FIRE));
        assertTrue(VehicleType.AMBULANCE.canAttend(IncidentType.ACCIDENT));
        assertFalse(VehicleType.AMBULANCE.canAttend(IncidentType.ROBBERY));
        assertTrue(VehicleType.FIRE_TRUCK.canAttend(IncidentType.FIRE));
        assertFalse(VehicleType.FIRE_TRUCK.canAttend(IncidentType.ACCIDENT));
    }

    @Test
    void repositoryFindsByIdAndFiltersAvailableCompatibleVehicles() {
        VehicleRepository vehicles = new VehicleRepository();
        vehicles.add(new Vehicle("P-1", VehicleType.PATROL, new Position(0, 0)));
        vehicles.add(new Vehicle("A-1", VehicleType.AMBULANCE, new Position(7, 5)));
        vehicles.add(new Vehicle("B-1", VehicleType.FIRE_TRUCK, new Position(7, 0)));
        vehicles.findById("A-1").setOutOfService(true);

        assertEquals("P-1", vehicles.findById("P-1").getId());
        assertThrows(VehicleNotFoundException.class, () -> vehicles.findById("X-9"));
        assertThrows(IllegalArgumentException.class,
                () -> vehicles.add(new Vehicle("P-1", VehicleType.PATROL, new Position(1, 0))));
        assertEquals(1, vehicles.availableFor(IncidentType.ACCIDENT).size());
        assertEquals(2, vehicles.availableCount());
    }

    @Test
    void operatorMovesOnlyOnTraversableCellsInsideTheMap() {
        OperatorNavigator operator = new OperatorNavigator(TestCity.map(), new Position(1, 1));

        operator.move(Direction.RIGHT, 0);
        assertEquals(new Position(2, 1), operator.getPosition());
        assertThrows(InvalidMovementException.class, () -> operator.move(Direction.DOWN, 1));

        OperatorNavigator corner = new OperatorNavigator(TestCity.map(), new Position(0, 0));
        assertThrows(InvalidMovementException.class, () -> corner.move(Direction.UP, 0));
        assertThrows(InvalidMovementException.class, () -> corner.move(Direction.LEFT, 0));
        assertEquals(0, corner.historySize());
    }

    @Test
    void undoRestoresPositionsInReverseOrderAndFailsWhenEmpty() {
        OperatorNavigator operator = new OperatorNavigator(TestCity.map(), new Position(0, 0));
        operator.move(Direction.RIGHT, 0);
        operator.move(Direction.RIGHT, 1);
        operator.move(Direction.DOWN, 2);

        assertEquals(Direction.DOWN, operator.undo().getDirection());
        assertEquals(new Position(2, 0), operator.getPosition());
        operator.undo();
        operator.undo();
        assertEquals(new Position(0, 0), operator.getPosition());
        assertThrows(EmptyStructureException.class, operator::undo);
    }

    @Test
    void vehicleFollowsItsRouteAndIsReleasedAfterAttending() {
        Vehicle ambulance = new Vehicle("A-1", VehicleType.AMBULANCE, new Position(0, 0));
        co.edu.icesi.sgmms.model.structures.LinkedList<Position> path = new co.edu.icesi.sgmms.model.structures.LinkedList<>();
        path.add(new Position(0, 0));
        path.add(new Position(1, 0));
        path.add(new Position(2, 0));

        ambulance.dispatch("INC-1", path);
        assertEquals(VehicleStatus.EN_ROUTE, ambulance.getStatus());
        assertEquals(2, ambulance.remainingSteps());
        ambulance.advance();
        ambulance.advance();
        assertEquals(new Position(2, 0), ambulance.getPosition());

        ambulance.startAttending();
        assertEquals(VehicleStatus.ATTENDING, ambulance.getStatus());
        ambulance.release();
        assertTrue(ambulance.isAvailable());
    }
}
