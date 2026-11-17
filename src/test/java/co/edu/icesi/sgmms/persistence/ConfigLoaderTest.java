package co.edu.icesi.sgmms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.ZoneType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    private static final String GRID = "[[4,4,4,4],[4,2,3,4],[4,0,1,4],[4,4,4,4]]";

    private static String city(String grid, String center, String extraRoutes) {
        return "{\"name\": \"Test\", \"grid\": " + grid + ", \"monitoringCenter\": " + center + ","
                + "\"routes\": {\"baseDistance\": 1.0, \"congestionStep\": 0.25, \"segmentFactors\": "
                + "{\"MAIN_ROAD\": 1.0, \"AVAILABLE_PATH\": 1.5, \"RESIDENTIAL\": 2.0, \"COMMERCIAL\": 2.5}"
                + extraRoutes + "}, \"zones\": {\"districtSize\": 2, \"zoneCapacity\": 3}}";
    }

    private static final String CITY = city(GRID, "{\"x\": 0, \"y\": 0}", "");

    private static String vehicles(String... entries) {
        return "{\"vehicles\": [" + String.join(",", entries) + "]}";
    }

    private static final String PATROL = "{\"id\": \"P-1\", \"type\": \"PATROL\", \"x\": 0, \"y\": 0}";
    private static final String FIRE = "{\"id\": \"B-1\", \"type\": \"FIRE_TRUCK\", \"x\": 3, \"y\": 3}";

    @Test
    void theBundledPalmiraConfigurationIsValid() throws Exception {
        SgmmsConfig config = ConfigLoader.loadDefault();

        assertEquals("Palmira, Valle del Cauca", config.getCityName());
        assertEquals(24, config.getMap().getWidth());
        assertEquals(16, config.getMap().getHeight());
        assertEquals(9, config.getVehicles().size());
        assertEquals(14, config.getSettings().getCivilianCars());
        assertTrue(config.getMap().cellsOf(ZoneType.COMMERCIAL).size() > 0);
    }

    @Test
    void aValidDocumentProducesMapVehiclesAndSettings() throws Exception {
        SgmmsConfig config = ConfigLoader.parse(CITY, vehicles(PATROL, FIRE,
                "{\"id\": \"A-1\", \"type\": \"AMBULANCE\", \"x\": 3, \"y\": 0, \"outOfService\": true}"));

        assertEquals(15, config.getMap().baseGraph().vertexCount());
        assertEquals(VehicleStatus.OUT_OF_SERVICE, config.getVehicles().findById("A-1").getStatus());
        assertEquals(2, config.getMap().getSettings().getDistrictSize());
    }

    @Test
    void missingFileAndMissingFieldAreReported(@TempDir Path folder) throws Exception {
        ConfigurationException missing = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.load(folder.resolve("nope.json"), folder.resolve("nope2.json")));
        assertTrue(missing.getMessage().contains("No existe"));

        Path city = folder.resolve("city.json");
        Files.writeString(city, "{\"grid\": " + GRID + "}");
        Path cars = folder.resolve("vehicles.json");
        Files.writeString(cars, vehicles(PATROL, FIRE));
        ConfigurationException field = assertThrows(ConfigurationException.class, () -> ConfigLoader.load(city, cars));
        assertTrue(field.getMessage().contains("monitoringCenter"), field.getMessage());
    }

    @Test
    void wrongTypesAndInconsistentGridsAreRejected() {
        assertThrows(ConfigurationException.class,
                () -> ConfigLoader.parse(city("[[4,\"x\"]]", "{\"x\": 0, \"y\": 0}", ""), vehicles(PATROL, FIRE)));
        ConfigurationException ragged = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.parse(city("[[4,4],[4]]", "{\"x\": 0, \"y\": 0}", ""), vehicles(PATROL, FIRE)));
        assertTrue(ragged.getMessage().contains("inconsistente"));
        assertThrows(ConfigurationException.class,
                () -> ConfigLoader.parse(city(GRID, "{\"x\": 2, \"y\": 2}", ""), vehicles(PATROL, FIRE)));
    }

    @Test
    void vehiclesMustBeUniqueKnownOnRoadsAndCoverEveryIncidentType() {
        assertThrows(ConfigurationException.class, () -> ConfigLoader.parse(CITY, vehicles(PATROL, PATROL, FIRE)));
        assertThrows(ConfigurationException.class, () -> ConfigLoader.parse(CITY,
                vehicles(PATROL, FIRE, "{\"id\": \"X\", \"type\": \"TANK\", \"x\": 0, \"y\": 0}")));
        assertThrows(ConfigurationException.class, () -> ConfigLoader.parse(CITY,
                vehicles(PATROL, FIRE, "{\"id\": \"A\", \"type\": \"AMBULANCE\", \"x\": 2, \"y\": 2}")));
        assertThrows(ConfigurationException.class, () -> ConfigLoader.parse(CITY,
                vehicles(PATROL, FIRE, "{\"id\": \"A\", \"type\": \"AMBULANCE\", \"x\": 9, \"y\": 9}")));
        ConfigurationException coverage = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.parse(CITY, vehicles(PATROL)));
        assertTrue(coverage.getMessage().contains("Incendio"), coverage.getMessage());
    }

    @Test
    void blockedRoadsInTheFileAreAppliedAndValidated() throws Exception {
        String blocked = ", \"blockedRoads\": [{\"from\": {\"x\": 0, \"y\": 0}, \"to\": {\"x\": 1, \"y\": 0}}]";
        SgmmsConfig config = ConfigLoader.parse(city(GRID, "{\"x\": 0, \"y\": 0}", blocked), vehicles(PATROL, FIRE));
        assertTrue(config.getMap().isBlocked(new Position(0, 0), new Position(1, 0)));

        String invalid = ", \"blockedRoads\": [{\"from\": {\"x\": 0, \"y\": 0}, \"to\": {\"x\": 3, \"y\": 3}}]";
        assertThrows(ConfigurationException.class,
                () -> ConfigLoader.parse(city(GRID, "{\"x\": 0, \"y\": 0}", invalid), vehicles(PATROL, FIRE)));
    }
}
