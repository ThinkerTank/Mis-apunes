package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * "Coverage of the monitoring center" for one kind of zone: how many road segments (BFS hops)
 * separate the center from the cells of that zone.
 */
public class ZoneCoverage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ZoneType zone;
    private final int cells;
    private final double averageHops;
    private final int maxHops;

    public ZoneCoverage(ZoneType zone, int cells, double averageHops, int maxHops) {
        this.zone = zone;
        this.cells = cells;
        this.averageHops = averageHops;
        this.maxHops = maxHops;
    }

    public ZoneType getZone() {
        return zone;
    }

    public int getCells() {
        return cells;
    }

    public double getAverageHops() {
        return averageHops;
    }

    public int getMaxHops() {
        return maxHops;
    }
}
