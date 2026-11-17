package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.ZoneType;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;
import java.util.Random;

/**
 * Generates robberies and fires at random (RF4), with a different probability per kind of zone:
 *
 * <pre>
 *                     commercial  residential  path  main road
 *   robbery weight        5            3        1.5     0.5
 *   fire weight           3            4        1       0
 * </pre>
 *
 * Robberies concentrate where there is commerce and money and are rare on main roads, which are
 * busy and watched. Fires start inside buildings — homes first, then shops — and never on the
 * roadway itself.
 */
public class IncidentGenerator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ZoneType[] ZONES = {
        ZoneType.COMMERCIAL, ZoneType.RESIDENTIAL, ZoneType.AVAILABLE_PATH, ZoneType.MAIN_ROAD,
    };
    private static final double[] ROBBERY_WEIGHTS = {5, 3, 1.5, 0.5};
    private static final double[] FIRE_WEIGHTS = {3, 4, 1, 0};

    private static final String[] ROBBERY_DESCRIPTIONS = {
        "Hurto a transeúnte", "Robo a establecimiento comercial", "Robo de vehículo", "Asalto a vivienda",
    };
    private static final String[] FIRE_DESCRIPTIONS = {
        "Incendio estructural", "Conato de incendio en cocina", "Cortocircuito con humo", "Incendio en bodega",
    };

    private transient HashTable<ZoneType, LinkedList<Position>> cellsByZone;

    /**
     * Possibly generates one robbery and one fire for this minute.
     *
     * <p>Complexity — time: O(n) to collect occupied cells plus O(1) expected to pick a free cell;
     * O(V) the first time, to group the cells by zone; space: O(V).
     *
     * @return the incidents generated (possibly none)
     */
    public LinkedList<Incident> tick(DispatchCenter center, SimulationSettings settings, Random random,
                                     HashTable<Position, Boolean> busyCells) {
        LinkedList<Incident> created = new LinkedList<>();
        if (center.indicators().getActiveIncidents() >= settings.getMaxActiveIncidents()) {
            return created;
        }
        CityMap map = center.getMap();
        if (random.nextDouble() < settings.getRobberyProbability()) {
            Position place = pickPosition(map, ROBBERY_WEIGHTS, random, busyCells);
            if (place != null) {
                created.addLast(center.reportIncident(IncidentType.ROBBERY, place,
                        severity(random, 0.50, 0.35), pick(ROBBERY_DESCRIPTIONS, random)));
                busyCells.put(place, Boolean.TRUE);
            }
        }
        if (random.nextDouble() < settings.getFireProbability()) {
            Position place = pickPosition(map, FIRE_WEIGHTS, random, busyCells);
            if (place != null) {
                created.addLast(center.reportIncident(IncidentType.FIRE, place,
                        severity(random, 0.30, 0.40), pick(FIRE_DESCRIPTIONS, random)));
                busyCells.put(place, Boolean.TRUE);
            }
        }
        return created;
    }

    /**
     * Picks a free cell: first a zone according to the weights (only zones that exist on the map),
     * then a uniformly random cell of that zone.
     *
     * <p>Complexity — time: O(1) expected (a few retries at most); space: O(1).
     */
    public Position pickPosition(CityMap map, double[] weights, Random random, HashTable<Position, Boolean> busy) {
        HashTable<ZoneType, LinkedList<Position>> cells = cells(map);
        double total = 0;
        for (int i = 0; i < ZONES.length; i++) {
            if (!cells.get(ZONES[i]).isEmpty()) {
                total += weights[i];
            }
        }
        if (total == 0) {
            return null;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            double roll = random.nextDouble() * total;
            for (int i = 0; i < ZONES.length; i++) {
                LinkedList<Position> zoneCells = cells.get(ZONES[i]);
                if (zoneCells.isEmpty() || weights[i] == 0) {
                    continue;
                }
                roll -= weights[i];
                if (roll <= 0) {
                    Position candidate = zoneCells.get(random.nextInt(zoneCells.size()));
                    if (!busy.containsKey(candidate)) {
                        return candidate;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /** Robbery zone weights, exposed for the tests and the documentation. */
    public static double[] robberyWeights() {
        return ROBBERY_WEIGHTS.clone();
    }

    /** Fire zone weights, exposed for the tests and the documentation. */
    public static double[] fireWeights() {
        return FIRE_WEIGHTS.clone();
    }

    private HashTable<ZoneType, LinkedList<Position>> cells(CityMap map) {
        if (cellsByZone == null) {
            cellsByZone = new HashTable<>();
            for (ZoneType zone : ZONES) {
                cellsByZone.put(zone, map.cellsOf(zone));
            }
        }
        return cellsByZone;
    }

    private static Severity severity(Random random, double low, double medium) {
        double roll = random.nextDouble();
        if (roll < low) {
            return Severity.LOW;
        }
        return roll < low + medium ? Severity.MEDIUM : Severity.HIGH;
    }

    private static String pick(String[] options, Random random) {
        return options[random.nextInt(options.length)];
    }
}
