package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.IncidentNotFoundException;
import co.edu.icesi.sgmms.model.structures.BinarySearchTree;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;

/**
 * Active incidents, stored twice and always kept consistent:
 * <ul>
 *   <li>a {@link BinarySearchTree} ordered by {@link IncidentPriorityComparator}, which answers
 *       "which is the most urgent?" and lists every incident in priority order (APO II);</li>
 *   <li>a {@link HashTable} indexed by incident ID, which answers "give me INC-0007" in O(1)
 *       expected time for the panel operations (CyED I, RF26).</li>
 * </ul>
 * Insertion and removal touch both structures inside one method, and the caller
 * ({@code DispatchCenter}) runs it while holding its lock, so no other thread can observe one
 * structure updated and the other not. Resolved incidents move to a history list.
 */
public class IncidentRepository implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BinarySearchTree<Incident> byPriority = new BinarySearchTree<>(new IncidentPriorityComparator());
    private final HashTable<String, Incident> byId = new HashTable<>();
    private final LinkedList<Incident> resolved = new LinkedList<>();

    /**
     * Adds an active incident to the tree and to the index.
     *
     * <p>Complexity — time: average O(log n) (tree) + O(1) (hash); worst O(n); space: O(1).
     *
     * @throws IllegalArgumentException if the ID is already registered
     */
    public void add(Incident incident) {
        if (byId.containsKey(incident.getId())) {
            throw new IllegalArgumentException("El id de incidente " + incident.getId() + " está repetido");
        }
        byPriority.insert(incident);
        byId.put(incident.getId(), incident);
    }

    /**
     * Finds an active incident by ID.
     *
     * <p>Complexity — time: average O(1), worst O(n); space: O(1).
     *
     * @throws IncidentNotFoundException if the ID is not active
     */
    public Incident findById(String id) {
        Incident incident = byId.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException("No existe un incidente activo con id " + id);
        }
        return incident;
    }

    /** Whether the ID belongs to an active incident. Complexity — average O(1). */
    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    /**
     * Removes a resolved incident from the tree and the index, and keeps it in the history.
     *
     * <p>Complexity — time: average O(log n) + O(1); worst O(n); space: O(1).
     */
    public Incident archive(String id) {
        Incident incident = findById(id);
        byPriority.remove(incident);
        byId.remove(id);
        resolved.addLast(incident);
        return incident;
    }

    /**
     * Active incident with the highest priority (the maximum of the tree), or {@code null}.
     *
     * <p>Complexity — time: average O(log n), worst O(n); space: O(1).
     */
    public Incident mostUrgent() {
        return byPriority.isEmpty() ? null : byPriority.max();
    }

    /**
     * Every active incident from the most to the least urgent.
     *
     * <p>Complexity — time: O(n); space: O(n).
     */
    public LinkedList<Incident> activeByPriority() {
        return byPriority.reverseInOrder();
    }

    /** Active incidents of a given severity, most urgent first. Complexity — O(n). */
    public LinkedList<Incident> activeWithSeverity(Severity severity) {
        return byPriority.findAll(incident -> incident.getSeverity() == severity);
    }

    /** Number of active incidents. Complexity — O(1). */
    public int activeCount() {
        return byPriority.size();
    }

    /** Number of active incidents of one type. Complexity — O(n). */
    public int activeCount(IncidentType type) {
        return activeByPriority().count(incident -> incident.getType() == type);
    }

    /** Resolved incidents in resolution order. Complexity — O(1). */
    public LinkedList<Incident> resolvedHistory() {
        return resolved;
    }

    /**
     * Checks that the tree and the index hold exactly the same incidents. Used by the tests.
     *
     * <p>Complexity — time: O(n); space: O(n).
     */
    public boolean isConsistent() {
        if (byPriority.size() != byId.size()) {
            return false;
        }
        for (Incident incident : byPriority.inOrder()) {
            if (byId.get(incident.getId()) != incident) {
                return false;
            }
        }
        return true;
    }
}
