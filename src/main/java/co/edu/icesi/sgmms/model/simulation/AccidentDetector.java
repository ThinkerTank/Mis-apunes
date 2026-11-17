package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.Direction;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;

/**
 * Applies the three {@link AccidentCriterion accident criteria} to the movements of one minute.
 *
 * <p>A criterion does not fire again for the same place (or district, for congestion) until the
 * cool-down has passed, and never on a cell that already has an active incident: a crash that is
 * already being attended does not produce a new crash every minute.
 */
public class AccidentDetector implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HashTable<String, Long> lastFired = new HashTable<>();
    private final int cooldownMinutes;

    public AccidentDetector(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    /**
     * Detects the accidents produced in one minute.
     *
     * <p>Complexity — time: O(k + D) for k movements and D districts (each movement checks its
     * cell and four neighbors against the O(1) risk-point lookup); space: O(k + D).
     *
     * @param moves     movements of every moving vehicle (civilian and emergency) this minute
     * @param busyCells cells that already have an active incident
     */
    public LinkedList<AccidentReport> detect(CityMap map, LinkedList<CarMove> moves,
                                             HashTable<Position, Boolean> busyCells, long minute) {
        LinkedList<AccidentReport> reports = new LinkedList<>();
        HashTable<String, Boolean> crashed = new HashTable<>();
        detectCollisions(moves, busyCells, minute, reports, crashed);
        detectRiskPointConvergence(map, moves, busyCells, minute, reports, crashed);
        detectCongestion(map, moves, busyCells, minute, reports);
        return reports;
    }

    /** Criterion 1: two moving vehicles end up in the same cell. */
    private void detectCollisions(LinkedList<CarMove> moves, HashTable<Position, Boolean> busy, long minute,
                                  LinkedList<AccidentReport> reports, HashTable<String, Boolean> crashed) {
        HashTable<Position, LinkedList<String>> byTarget = new HashTable<>();
        LinkedList<Position> targets = new LinkedList<>();
        for (CarMove move : moves) {
            if (!move.moved()) {
                continue;
            }
            LinkedList<String> group = byTarget.get(move.getTo());
            if (group == null) {
                group = new LinkedList<>();
                byTarget.put(move.getTo(), group);
                targets.addLast(move.getTo());
            }
            group.addLast(move.getVehicleId());
        }
        for (Position target : targets) {
            LinkedList<String> group = byTarget.get(target);
            if (group.size() >= 2 && fire(AccidentCriterion.COLLISION, "collision@" + target, target, group, busy,
                    minute, reports)) {
                markCrashed(group, crashed);
            }
        }
    }

    /** Criterion 2: a district holds more vehicles than its capacity. */
    private void detectCongestion(CityMap map, LinkedList<CarMove> moves, HashTable<Position, Boolean> busy,
                                  long minute, LinkedList<AccidentReport> reports) {
        int[] counts = new int[map.districtCount()];
        for (CarMove move : moves) {
            counts[map.districtOf(move.getTo())]++;
        }
        int capacity = map.getSettings().getZoneCapacity();
        for (int district = 0; district < counts.length; district++) {
            if (counts[district] > capacity) {
                LinkedList<String> involved = new LinkedList<>();
                Position place = null;
                for (CarMove move : moves) {
                    if (map.districtOf(move.getTo()) == district) {
                        involved.addLast(move.getVehicleId());
                        if (place == null || busy.containsKey(place)) {
                            place = move.getTo();
                        }
                    }
                }
                fire(AccidentCriterion.CRITICAL_CONGESTION, "congestion@" + district, place, involved, busy,
                        minute, reports);
            }
        }
    }

    /**
     * Criterion 3: vehicles from different directions converge on the same risk point. A vehicle
     * converges on point r when it lands on r, or on a neighbor of r while getting closer to it.
     */
    private void detectRiskPointConvergence(CityMap map, LinkedList<CarMove> moves, HashTable<Position, Boolean> busy,
                                            long minute, LinkedList<AccidentReport> reports,
                                            HashTable<String, Boolean> crashed) {
        HashTable<Position, LinkedList<CarMove>> arriving = new HashTable<>();
        LinkedList<Position> points = new LinkedList<>();
        for (CarMove move : moves) {
            if (!move.moved() || crashed.containsKey(move.getVehicleId())) {
                continue;
            }
            for (Position candidate : around(move.getTo())) {
                boolean approaching = candidate.manhattan(move.getTo()) < candidate.manhattan(move.getFrom());
                if (approaching && map.isInside(candidate) && map.canEnter(candidate) && map.isRiskPoint(candidate)) {
                    LinkedList<CarMove> group = arriving.get(candidate);
                    if (group == null) {
                        group = new LinkedList<>();
                        arriving.put(candidate, group);
                        points.addLast(candidate);
                    }
                    group.addLast(move);
                }
            }
        }
        for (Position point : points) {
            LinkedList<CarMove> group = arriving.get(point);
            group.removeIf(move -> crashed.containsKey(move.getVehicleId()));
            if (group.size() >= 2 && differentOrigins(group)) {
                LinkedList<String> involved = new LinkedList<>();
                for (CarMove move : group) {
                    involved.addLast(move.getVehicleId());
                }
                if (fire(AccidentCriterion.RISK_POINT_CONVERGENCE, "risk@" + point, point, involved, busy, minute,
                        reports)) {
                    markCrashed(involved, crashed);
                }
            }
        }
    }

    private boolean fire(AccidentCriterion criterion, String key, Position place, LinkedList<String> involved,
                         HashTable<Position, Boolean> busy, long minute, LinkedList<AccidentReport> reports) {
        if (place == null || busy.containsKey(place)) {
            return false;
        }
        Long last = lastFired.get(key);
        if (last != null && minute - last < cooldownMinutes) {
            return false;
        }
        lastFired.put(key, minute);
        busy.put(place, Boolean.TRUE);
        reports.addLast(new AccidentReport(criterion, place, involved));
        return true;
    }

    private static void markCrashed(LinkedList<String> vehicles, HashTable<String, Boolean> crashed) {
        for (String id : vehicles) {
            crashed.put(id, Boolean.TRUE);
        }
    }

    private static Position[] around(Position p) {
        Position[] cells = new Position[5];
        cells[0] = p;
        int i = 1;
        for (Direction d : Direction.values()) {
            cells[i++] = p.step(d);
        }
        return cells;
    }

    private static boolean differentOrigins(LinkedList<CarMove> group) {
        Position first = group.getFirst().getFrom();
        return group.find(move -> !move.getFrom().equals(first)) != null;
    }
}
