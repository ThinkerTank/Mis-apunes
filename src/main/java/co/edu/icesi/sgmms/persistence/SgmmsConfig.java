package co.edu.icesi.sgmms.persistence;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.VehicleRepository;
import co.edu.icesi.sgmms.model.simulation.Simulation;
import co.edu.icesi.sgmms.model.simulation.SimulationSettings;

/** Everything read from the JSON configuration files, ready to start a simulation. */
public final class SgmmsConfig {

    private final String cityName;
    private final CityMap map;
    private final VehicleRepository vehicles;
    private final SimulationSettings settings;

    public SgmmsConfig(String cityName, CityMap map, VehicleRepository vehicles, SimulationSettings settings) {
        this.cityName = cityName;
        this.map = map;
        this.vehicles = vehicles;
        this.settings = settings;
    }

    /**
     * Builds a fresh simulation from this configuration.
     *
     * <p>Complexity — time O(V + E + c): places c private cars and applies the first congestion update over every road segment; space O(c).
     */
    public Simulation createSimulation() {
        Simulation simulation = new Simulation(new DispatchCenter(map, vehicles), settings);
        simulation.setCityName(cityName);
        return simulation;
    }

    public String getCityName() {
        return cityName;
    }

    /** Complexity — time O(1), space O(1). */
    public CityMap getMap() {
        return map;
    }

    public VehicleRepository getVehicles() {
        return vehicles;
    }

    public SimulationSettings getSettings() {
        return settings;
    }
}
