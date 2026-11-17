package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.MapSettings;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleRepository;
import co.edu.icesi.sgmms.model.VehicleType;

/** Map and objects shared by the simulation tests. */
final class SimFixtures {

    static final int[][] GRID = {
        {4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
        {4, 2, 2, 0, 3, 3, 0, 2, 2, 4},
        {4, 2, 1, 0, 3, 1, 0, 2, 1, 4},
        {4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
        {4, 0, 2, 2, 0, 3, 3, 0, 3, 4},
        {4, 3, 3, 0, 2, 2, 0, 3, 3, 4},
        {4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
    };

    private SimFixtures() {
    }

    static CityMap map() {
        return new CityMap(GRID, new Position(0, 0), new MapSettings());
    }

    static CityMap map(MapSettings settings) {
        return new CityMap(GRID, new Position(0, 0), settings);
    }

    static DispatchCenter center() {
        VehicleRepository vehicles = new VehicleRepository();
        vehicles.add(new Vehicle("P-1", VehicleType.PATROL, new Position(0, 0)));
        vehicles.add(new Vehicle("P-2", VehicleType.PATROL, new Position(9, 6)));
        vehicles.add(new Vehicle("A-1", VehicleType.AMBULANCE, new Position(9, 0)));
        vehicles.add(new Vehicle("B-1", VehicleType.FIRE_TRUCK, new Position(0, 6)));
        return new DispatchCenter(map(), vehicles);
    }

    static SimulationSettings settings(long seed) {
        SimulationSettings settings = new SimulationSettings();
        settings.setSeed(seed);
        return settings;
    }
}
