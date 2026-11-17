package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/** Points earned for one resolved incident, broken down by rule so the operator sees why. */
public class ScoreBreakdown implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int base;
    private final int timeBonus;
    private final int routingBonus;
    private final int routingPenalty;

    public ScoreBreakdown(int base, int timeBonus, int routingBonus, int routingPenalty) {
        this.base = base;
        this.timeBonus = timeBonus;
        this.routingBonus = routingBonus;
        this.routingPenalty = routingPenalty;
    }

    /** Total points; never negative. Complexity — O(1). */
    public int total() {
        return Math.max(0, base + timeBonus + routingBonus - routingPenalty);
    }

    public int getBase() {
        return base;
    }

    public int getTimeBonus() {
        return timeBonus;
    }

    public int getRoutingBonus() {
        return routingBonus;
    }

    public int getRoutingPenalty() {
        return routingPenalty;
    }

    @Override
    public String toString() {
        return total() + " pts (base " + base + ", tiempo +" + timeBonus + ", ruta +" + routingBonus
                + ", penalización -" + routingPenalty + ")";
    }
}
