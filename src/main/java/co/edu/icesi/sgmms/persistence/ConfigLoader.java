package co.edu.icesi.sgmms.persistence;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.MapSettings;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleRepository;
import co.edu.icesi.sgmms.model.VehicleType;
import co.edu.icesi.sgmms.model.exceptions.SgmmsException;
import co.edu.icesi.sgmms.model.simulation.SimulationSettings;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the initial configuration from JSON (RF15): the city map, its routes and the vehicles.
 *
 * <p>Three levels of validation are applied, and each failure becomes a
 * {@link ConfigurationException} with a message that names the file and the field:
 * <ol>
 *   <li><b>Existence</b> — the file exists and can be read.</li>
 *   <li><b>Structure</b> — valid JSON, required fields present and of the expected type.</li>
 *   <li><b>Consistency</b> — rectangular grid with codes 0 to 4, monitoring center on a road,
 *       every zone reachable (BFS), blocked roads that exist, unique vehicle IDs, known vehicle
 *       types, vehicles on roads inside the map, and at least one vehicle able to attend each kind
 *       of incident.</li>
 * </ol>
 */
public final class ConfigLoader {

    /** Classpath location of the default city configuration. */
    public static final String DEFAULT_CITY = "/co/edu/icesi/sgmms/config/city.json";
    /** Classpath location of the default vehicle configuration. */
    public static final String DEFAULT_VEHICLES = "/co/edu/icesi/sgmms/config/vehicles.json";

    private ConfigLoader() {
    }

    /**
     * Loads the configuration bundled with the application.
     *
     * @throws ConfigurationException if a bundled file is missing or invalid
     */
    public static SgmmsConfig loadDefault() throws ConfigurationException {
        return parse(readResource(DEFAULT_CITY), readResource(DEFAULT_VEHICLES));
    }

    /**
     * Loads the configuration from two files chosen by the user.
     *
     * @throws ConfigurationException if a file is missing, unreadable or invalid
     */
    public static SgmmsConfig load(Path cityFile, Path vehiclesFile) throws ConfigurationException {
        return parse(read(cityFile), read(vehiclesFile));
    }

    /**
     * Parses and validates both documents.
     *
     * <p>Complexity — time: O(n) to parse n characters plus O(V²) to build the map's adjacency
     * matrix; space: O(V²).
     *
     * @throws ConfigurationException if the structure or the content is invalid
     */
    public static SgmmsConfig parse(String cityJson, String vehiclesJson) throws ConfigurationException {
        JsonValue city = JsonParser.parse(cityJson);
        requireKind(city.isObject(), "city.json", "el documento debe ser un objeto JSON");
        String name = city.has("name") ? requireString(city, "name", "city.json") : "Palmira";
        CityMap map = parseMap(city);
        SimulationSettings settings = parseSimulation(city);
        VehicleRepository vehicles = parseVehicles(JsonParser.parse(vehiclesJson), map);
        return new SgmmsConfig(name, map, vehicles, settings);
    }

    // ---------------------------------------------------------------- city.json

    private static CityMap parseMap(JsonValue city) throws ConfigurationException {
        JsonValue gridValue = requireArray(city, "grid", "city.json");
        if (gridValue.elements().isEmpty()) {
            throw new ConfigurationException("city.json: \"grid\" no puede estar vacía");
        }
        int[][] grid = new int[gridValue.elements().size()][];
        int row = 0;
        for (JsonValue rowValue : gridValue.elements()) {
            requireKind(rowValue.isArray(), "city.json", "la fila " + row + " de \"grid\" debe ser un arreglo");
            grid[row] = new int[rowValue.elements().size()];
            int column = 0;
            for (JsonValue cell : rowValue.elements()) {
                requireKind(cell.isInteger(), "city.json",
                        "la celda (" + column + "," + row + ") de \"grid\" debe ser un entero entre 0 y 4");
                grid[row][column++] = cell.asInt();
            }
            row++;
        }
        JsonValue center = requireObject(city, "monitoringCenter", "city.json");
        Position monitoringCenter = new Position(requireInt(center, "x", "city.json monitoringCenter"),
                requireInt(center, "y", "city.json monitoringCenter"));

        MapSettings settings = new MapSettings();
        JsonValue routes = requireObject(city, "routes", "city.json");
        settings.setBaseDistance(requireNumber(routes, "baseDistance", "city.json routes"));
        settings.setCongestionStep(requireNumber(routes, "congestionStep", "city.json routes"));
        JsonValue factors = requireObject(routes, "segmentFactors", "city.json routes");
        settings.setFactors(requireNumber(factors, "MAIN_ROAD", "city.json segmentFactors"),
                requireNumber(factors, "AVAILABLE_PATH", "city.json segmentFactors"),
                requireNumber(factors, "RESIDENTIAL", "city.json segmentFactors"),
                requireNumber(factors, "COMMERCIAL", "city.json segmentFactors"));
        JsonValue zones = requireObject(city, "zones", "city.json");
        settings.setDistrictSize(requireInt(zones, "districtSize", "city.json zones"));
        settings.setZoneCapacity(requireInt(zones, "zoneCapacity", "city.json zones"));

        CityMap map;
        try {
            map = new CityMap(grid, monitoringCenter, settings);
        } catch (SgmmsException inconsistent) {
            throw new ConfigurationException("city.json es inconsistente: " + inconsistent.getMessage(), inconsistent);
        }
        if (routes.has("blockedRoads")) {
            JsonValue blocked = requireArray(routes, "blockedRoads", "city.json routes");
            for (JsonValue road : blocked.elements()) {
                requireKind(road.isObject(), "city.json", "cada vía bloqueada debe ser un objeto {from, to}");
                JsonValue from = requireObject(road, "from", "city.json blockedRoads");
                JsonValue to = requireObject(road, "to", "city.json blockedRoads");
                Position a = new Position(requireInt(from, "x", "blockedRoads.from"), requireInt(from, "y", "blockedRoads.from"));
                Position b = new Position(requireInt(to, "x", "blockedRoads.to"), requireInt(to, "y", "blockedRoads.to"));
                try {
                    map.blockRoad(a, b);
                } catch (SgmmsException invalid) {
                    throw new ConfigurationException("city.json: vía bloqueada inválida: " + invalid.getMessage(), invalid);
                }
            }
        }
        return map;
    }

    private static SimulationSettings parseSimulation(JsonValue city) throws ConfigurationException {
        SimulationSettings settings = new SimulationSettings();
        if (!city.has("simulation")) {
            return settings;
        }
        JsonValue sim = requireObject(city, "simulation", "city.json");
        String where = "city.json simulation";
        if (sim.has("robberyProbability")) {
            settings.setRobberyProbability(probability(sim, "robberyProbability", where));
        }
        if (sim.has("fireProbability")) {
            settings.setFireProbability(probability(sim, "fireProbability", where));
        }
        if (sim.has("collisionProbability")) {
            settings.setCollisionProbability(probability(sim, "collisionProbability", where));
        }
        if (sim.has("civilianCars")) {
            settings.setCivilianCars(nonNegative(sim, "civilianCars", where));
        }
        if (sim.has("maxActiveIncidents")) {
            settings.setMaxActiveIncidents(Math.max(1, nonNegative(sim, "maxActiveIncidents", where)));
        }
        if (sim.has("accidentCooldownMinutes")) {
            settings.setAccidentCooldownMinutes(nonNegative(sim, "accidentCooldownMinutes", where));
        }
        return settings;
    }

    // ---------------------------------------------------------------- vehicles.json

    private static VehicleRepository parseVehicles(JsonValue root, CityMap map) throws ConfigurationException {
        requireKind(root.isObject(), "vehicles.json", "el documento debe ser un objeto JSON");
        JsonValue list = requireArray(root, "vehicles", "vehicles.json");
        if (list.elements().isEmpty()) {
            throw new ConfigurationException("vehicles.json: debe haber al menos un vehículo");
        }
        VehicleRepository vehicles = new VehicleRepository();
        int index = 0;
        for (JsonValue item : list.elements()) {
            String where = "vehicles.json vehículo #" + index++;
            requireKind(item.isObject(), "vehicles.json", "cada vehículo debe ser un objeto");
            String id = requireString(item, "id", where).trim();
            if (id.isEmpty()) {
                throw new ConfigurationException(where + ": el id no puede estar vacío");
            }
            if (vehicles.contains(id)) {
                throw new ConfigurationException("vehicles.json: el id " + id + " está repetido");
            }
            VehicleType type;
            String typeName = requireString(item, "type", where);
            try {
                type = VehicleType.valueOf(typeName);
            } catch (IllegalArgumentException unknown) {
                throw new ConfigurationException(where + ": tipo desconocido \"" + typeName
                        + "\" (use PATROL, AMBULANCE o FIRE_TRUCK)");
            }
            Position position = new Position(requireInt(item, "x", where), requireInt(item, "y", where));
            if (!map.isInside(position)) {
                throw new ConfigurationException(where + " (" + id + "): la posición " + position + " está fuera del mapa");
            }
            if (!map.canEnter(position)) {
                throw new ConfigurationException(where + " (" + id + "): la posición " + position + " es un obstáculo");
            }
            Vehicle vehicle = new Vehicle(id, type, position);
            if (item.has("outOfService") && requireBoolean(item, "outOfService", where)) {
                vehicle.setOutOfService(true);
            }
            vehicles.add(vehicle);
        }
        HashTable<IncidentType, Boolean> covered = new HashTable<>();
        for (Vehicle vehicle : vehicles.all()) {
            for (IncidentType incident : IncidentType.values()) {
                if (vehicle.canAttend(incident)) {
                    covered.put(incident, Boolean.TRUE);
                }
            }
        }
        LinkedList<String> missing = new LinkedList<>();
        for (IncidentType incident : IncidentType.values()) {
            if (!covered.containsKey(incident)) {
                missing.addLast(incident.getLabel());
            }
        }
        if (!missing.isEmpty()) {
            throw new ConfigurationException("vehicles.json: ningún vehículo puede atender " + missing);
        }
        return vehicles;
    }

    // ---------------------------------------------------------------- reading files

    static String read(Path file) throws ConfigurationException {
        if (file == null || !Files.exists(file)) {
            throw new ConfigurationException("No existe el archivo de configuración: " + file);
        }
        if (!Files.isReadable(file) || Files.isDirectory(file)) {
            throw new ConfigurationException("No se puede leer el archivo de configuración: " + file);
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new ConfigurationException("Error leyendo " + file + ": " + error.getMessage(), error);
        }
    }

    static String readResource(String path) throws ConfigurationException {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new ConfigurationException("No se encontró el recurso de configuración " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new ConfigurationException("Error leyendo el recurso " + path + ": " + error.getMessage(), error);
        }
    }

    // ---------------------------------------------------------------- field helpers

    private static JsonValue require(JsonValue parent, String key, String where) throws ConfigurationException {
        if (!parent.has(key) || parent.get(key).isNull()) {
            throw new ConfigurationException(where + ": falta el campo obligatorio \"" + key + "\"");
        }
        return parent.get(key);
    }

    private static JsonValue requireObject(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isObject(), where, "\"" + key + "\" debe ser un objeto");
        return value;
    }

    private static JsonValue requireArray(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isArray(), where, "\"" + key + "\" debe ser un arreglo");
        return value;
    }

    private static String requireString(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isString(), where, "\"" + key + "\" debe ser un texto");
        return value.asString();
    }

    private static int requireInt(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isInteger(), where, "\"" + key + "\" debe ser un número entero");
        return value.asInt();
    }

    private static double requireNumber(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isNumber(), where, "\"" + key + "\" debe ser un número");
        return value.asDouble();
    }

    private static boolean requireBoolean(JsonValue parent, String key, String where) throws ConfigurationException {
        JsonValue value = require(parent, key, where);
        requireKind(value.isBoolean(), where, "\"" + key + "\" debe ser true o false");
        return value.asBoolean();
    }

    private static double probability(JsonValue parent, String key, String where) throws ConfigurationException {
        double value = requireNumber(parent, key, where);
        if (value < 0 || value > 1) {
            throw new ConfigurationException(where + ": \"" + key + "\" debe estar entre 0 y 1");
        }
        return value;
    }

    private static int nonNegative(JsonValue parent, String key, String where) throws ConfigurationException {
        int value = requireInt(parent, key, where);
        if (value < 0) {
            throw new ConfigurationException(where + ": \"" + key + "\" no puede ser negativo");
        }
        return value;
    }

    private static void requireKind(boolean ok, String where, String message) throws ConfigurationException {
        if (!ok) {
            throw new ConfigurationException(where + ": " + message);
        }
    }
}
