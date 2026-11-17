package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * Scoring rules of the operator.
 *
 * <pre>
 *   total = base(severity) + time bonus + routing bonus − routing penalty     (never below 0)
 *
 *   base(severity)  = 100 high | 70 medium | 40 low
 *   time bonus      = +20 if (resolution minute − generation minute) ≤ max time of the severity
 *                         (15 high | 25 medium | 40 low)
 *   routing bonus   = +10 if the vehicle sent had the cheapest Dijkstra route among the compatible
 *                         vehicles available at assignment time
 *   routing penalty = −15 if the vehicle sent was more than 1.5 times farther than the best one
 *                         according to the Floyd-Warshall matrix
 * </pre>
 *
 * Rejected assignments (incompatible or unavailable vehicle) throw an exception before any record
 * is created, so they never earn points.
 */
public class ScoreCalculator implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TIME_BONUS = 20;
    public static final int ROUTING_BONUS = 10;
    public static final int ROUTING_PENALTY = 15;
    public static final double PENALTY_FACTOR = 1.5;

    /**
     * Computes the points of a resolved incident.
     *
     * <p>Complexity — time and space O(1).
     *
     * @param record the assignment decision; {@code null} only for incidents resolved without one
     */
    public ScoreBreakdown score(Incident incident, AssignmentRecord record, long resolvedMinute) {
        Severity severity = incident.getSeverity();
        long elapsed = resolvedMinute - incident.getGeneratedAtMinute();
        int timeBonus = elapsed <= severity.getMaxAttentionMinutes() ? TIME_BONUS : 0;
        int routingBonus = 0;
        int routingPenalty = 0;
        if (record != null) {
            routingBonus = record.choseCheapestRoute() ? ROUTING_BONUS : 0;
            routingPenalty = record.exceedsOptimum(PENALTY_FACTOR) ? ROUTING_PENALTY : 0;
        }
        return new ScoreBreakdown(severity.getBasePoints(), timeBonus, routingBonus, routingPenalty);
    }
}
