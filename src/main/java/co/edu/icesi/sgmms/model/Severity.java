package co.edu.icesi.sgmms.model;

/**
 * Severity of an incident. The order of the constants is the priority order (High &gt; Medium &gt;
 * Low), and each level carries its base score and the maximum attention time that earns the
 * 20-point bonus.
 *
 * <p>Maximum times are in simulated minutes. Crossing the whole map along main roads takes about
 * 25 minutes, so 15 minutes for a high-severity incident demands that the operator choose a close
 * vehicle and act right away; 25 and 40 minutes leave room for medium and low severity incidents
 * to wait behind the urgent ones.
 */
public enum Severity {
    LOW(1, 40, 40, "Baja"),
    MEDIUM(2, 70, 25, "Media"),
    HIGH(3, 100, 15, "Alta");

    private final int level;
    private final int basePoints;
    private final int maxAttentionMinutes;
    private final String label;

    Severity(int level, int basePoints, int maxAttentionMinutes, String label) {
        this.level = level;
        this.basePoints = basePoints;
        this.maxAttentionMinutes = maxAttentionMinutes;
        this.label = label;
    }

    /** 1 for low, 2 for medium, 3 for high. */
    public int getLevel() {
        return level;
    }

    /** Points earned when an incident of this severity is resolved: 100, 70 or 40. */
    public int getBasePoints() {
        return basePoints;
    }

    /** Maximum attention time (simulated minutes) that still earns the time bonus. */
    public int getMaxAttentionMinutes() {
        return maxAttentionMinutes;
    }

    public String getLabel() {
        return label;
    }
}
