package co.edu.icesi.sgmms.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Priority order of incidents, shared by the binary search tree (APO II) and the priority queue
 * (CyED I) so both courses use exactly the same criterion.
 *
 * <p>{@code compare(a, b) > 0} means {@code a} is more urgent than {@code b}:
 * <ol>
 *   <li>higher severity first (High &gt; Medium &gt; Low);</li>
 *   <li>on a tie, the older incident first (smaller generation minute);</li>
 *   <li>on a tie, the one created first (smaller creation sequence), so two different incidents
 *       never compare equal and the tree never mistakes them for duplicates.</li>
 * </ol>
 */
public class IncidentPriorityComparator implements Comparator<Incident>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Complexity — time and space O(1). */
    @Override
    public int compare(Incident a, Incident b) {
        int bySeverity = Integer.compare(a.getSeverity().getLevel(), b.getSeverity().getLevel());
        if (bySeverity != 0) {
            return bySeverity;
        }
        int byAge = Long.compare(b.getGeneratedAtMinute(), a.getGeneratedAtMinute());
        if (byAge != 0) {
            return byAge;
        }
        return Long.compare(b.getSequence(), a.getSequence());
    }
}
