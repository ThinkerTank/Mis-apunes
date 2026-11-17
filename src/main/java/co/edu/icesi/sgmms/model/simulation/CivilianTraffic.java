package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.ZoneType;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;
import java.util.Random;

/**
 * Private cars that drive around the city. They are the moving elements whose behaviour produces
 * accidents and congestion. Each minute every car moves to an open neighboring cell, avoiding
 * going straight back and preferring main roads three to one; a car involved in an accident stays
 * still for a few minutes.
 */
public class CivilianTraffic implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int STOP_AFTER_ACCIDENT = 4;

    private final HashTable<String, Position> positions = new HashTable<>();
    private final HashTable<String, Position> previous = new HashTable<>();
    private final HashTable<String, Integer> stopped = new HashTable<>();
    private final LinkedList<String> ids = new LinkedList<>();

    /**
     * Places {@code count} cars on random main-road cells (or any road if there are none).
     *
     * <p>Complexity — time: O(V + count); space: O(count).
     */
    public CivilianTraffic(CityMap map, int count, Random random) {
        LinkedList<Position> starts = map.cellsOf(ZoneType.MAIN_ROAD);
        if (starts.isEmpty()) {
            starts = map.traversableCells();
        }
        for (int i = 1; i <= count; i++) {
            String id = String.format("CIV-%02d", i);
            Position start = starts.get(random.nextInt(starts.size()));
            ids.addLast(id);
            positions.put(id, start);
            previous.put(id, start);
            stopped.put(id, 0);
        }
    }

    /**
     * Moves every car one cell and returns the movements of this minute.
     *
     * <p>Complexity — time: O(c) for c cars (each looks at ≤ 4 neighbors); space: O(c).
     */
    public LinkedList<CarMove> step(CityMap map, Random random) {
        LinkedList<CarMove> moves = new LinkedList<>();
        for (String id : ids) {
            Position from = positions.get(id);
            int wait = stopped.get(id);
            if (wait > 0) {
                stopped.put(id, wait - 1);
                moves.addLast(new CarMove(id, from, from));
                continue;
            }
            Position to = chooseNext(map, from, previous.get(id), random);
            previous.put(id, from);
            positions.put(id, to);
            moves.addLast(new CarMove(id, from, to));
        }
        return moves;
    }

    /** Stops a car for a few minutes (it was involved in an accident). Complexity — O(1). */
    public void stop(String id) {
        if (stopped.containsKey(id)) {
            stopped.put(id, STOP_AFTER_ACCIDENT);
        }
    }

    /** Whether the car is currently stopped. Complexity — O(1). */
    public boolean isStopped(String id) {
        Integer wait = stopped.get(id);
        return wait != null && wait > 0;
    }

    /** Current positions of every car. Complexity — O(c). */
    public LinkedList<Position> positions() {
        LinkedList<Position> result = new LinkedList<>();
        for (String id : ids) {
            result.addLast(positions.get(id));
        }
        return result;
    }

    /** Position of one car. Complexity — O(1). */
    public Position positionOf(String id) {
        return positions.get(id);
    }

    public int size() {
        return ids.size();
    }

    private Position chooseNext(CityMap map, Position from, Position cameFrom, Random random) {
        LinkedList<Position> options = map.openNeighbors(from);
        if (options.isEmpty()) {
            return from;
        }
        if (options.size() > 1) {
            options.remove(cameFrom);
        }
        double total = 0;
        for (Position option : options) {
            total += weight(map, option);
        }
        double pick = random.nextDouble() * total;
        for (Position option : options) {
            pick -= weight(map, option);
            if (pick <= 0) {
                return option;
            }
        }
        return options.getLast();
    }

    private static double weight(CityMap map, Position p) {
        return map.zoneAt(p) == ZoneType.MAIN_ROAD ? 3.0 : 1.0;
    }
}
