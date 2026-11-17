package co.edu.icesi.sgmms.model;

import java.time.LocalDateTime;

/** Small maps and objects shared by the model tests. */
final class TestCity {

    /**
     * 8 × 6 map: main roads on rows 0, 3, 5 and columns 0, 7; two obstacles at (2,2) and (5,2).
     *
     * <pre>
     * y0: 4 4 4 4 4 4 4 4
     * y1: 4 2 2 0 3 3 0 4
     * y2: 4 2 1 0 3 1 0 4
     * y3: 4 4 4 4 4 4 4 4
     * y4: 4 0 2 2 0 3 3 4
     * y5: 4 4 4 4 4 4 4 4
     * </pre>
     */
    static final int[][] GRID = {
        {4, 4, 4, 4, 4, 4, 4, 4},
        {4, 2, 2, 0, 3, 3, 0, 4},
        {4, 2, 1, 0, 3, 1, 0, 4},
        {4, 4, 4, 4, 4, 4, 4, 4},
        {4, 0, 2, 2, 0, 3, 3, 4},
        {4, 4, 4, 4, 4, 4, 4, 4},
    };

    private TestCity() {
    }

    static CityMap map() {
        return new CityMap(GRID, new Position(0, 0), new MapSettings());
    }

    static Incident incident(String id, IncidentType type, Severity severity, long minute, long sequence) {
        return new Incident(id, type, new Position(3, 3), severity, "test incident",
                LocalDateTime.of(2026, 11, 17, 8, 0).plusMinutes(minute), minute, sequence);
    }
}
