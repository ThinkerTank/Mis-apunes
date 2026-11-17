package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.VehicleNotFoundException;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.Serializable;

/**
 * Registry of emergency vehicles in a {@link HashTable} keyed by vehicle ID (RF25), so looking a
 * vehicle up, assigning it and changing its state cost O(1) expected time. A list keeps the
 * configuration order for stable display.
 */
public class VehicleRepository implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HashTable<String, Vehicle> byId = new HashTable<>();
    private final LinkedList<Vehicle> inOrder = new LinkedList<>();

    /**
     * Registers a vehicle.
     *
     * <p>Complexity — time: average O(1), worst O(n); space: O(1).
     *
     * @throws IllegalArgumentException if the ID is already registered
     */
    public void add(Vehicle vehicle) {
        if (byId.containsKey(vehicle.getId())) {
            throw new IllegalArgumentException("Duplicate vehicle id " + vehicle.getId());
        }
        byId.put(vehicle.getId(), vehicle);
        inOrder.addLast(vehicle);
    }

    /**
     * Finds a vehicle by ID.
     *
     * <p>Complexity — time: average O(1), worst O(n); space: O(1).
     *
     * @throws VehicleNotFoundException if the ID does not exist
     */
    public Vehicle findById(String id) {
        Vehicle vehicle = byId.get(id);
        if (vehicle == null) {
            throw new VehicleNotFoundException("There is no vehicle with id " + id);
        }
        return vehicle;
    }

    /** Whether a vehicle with that ID exists. Complexity — average O(1). */
    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    /** Every vehicle in configuration order. Complexity — O(1) (the internal list). */
    public LinkedList<Vehicle> all() {
        return inOrder;
    }

    /** Available vehicles whose type can attend {@code type}. Complexity — O(n). */
    public LinkedList<Vehicle> availableFor(IncidentType type) {
        return inOrder.filter(v -> v.isAvailable() && v.canAttend(type));
    }

    /** Number of available vehicles. Complexity — O(n). */
    public int availableCount() {
        return inOrder.count(Vehicle::isAvailable);
    }

    /** Number of registered vehicles. Complexity — O(1). */
    public int size() {
        return inOrder.size();
    }
}
