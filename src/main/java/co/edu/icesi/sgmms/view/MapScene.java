package co.edu.icesi.sgmms.view;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.structures.Edge;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/**
 * Everything the renderer needs for one frame, collected from the model by the controller before
 * drawing, so the drawing code never touches the live model while other threads modify it.
 */
public final class MapScene {

    CityMap map;
    LinkedList<Edge<Position>> blocked = new LinkedList<>();
    LinkedList<Edge<Position>> corridors = new LinkedList<>();
    LinkedList<Position> riskPoints = new LinkedList<>();
    LinkedList<Vehicle> vehicles = new LinkedList<>();
    HashTable<String, LinkedList<Position>> routes = new HashTable<>();
    LinkedList<Position> civilians = new LinkedList<>();
    LinkedList<Incident> incidents = new LinkedList<>();
    Position operator;
    Position selected;
    Position pendingBlock;
    String highlightedVehicle;

    /** Complexity — time O(1), space O(1). */
    public MapScene map(CityMap value) {
        map = value;
        return this;
    }

    public MapScene blocked(LinkedList<Edge<Position>> value) {
        blocked = value;
        return this;
    }

    public MapScene corridors(LinkedList<Edge<Position>> value) {
        corridors = value;
        return this;
    }

    public MapScene riskPoints(LinkedList<Position> value) {
        riskPoints = value;
        return this;
    }

    /** Complexity — time O(1), space O(1). */
    public MapScene vehicles(LinkedList<Vehicle> value) {
        vehicles = value;
        return this;
    }

    public MapScene route(String vehicleId, LinkedList<Position> path) {
        routes.put(vehicleId, path);
        return this;
    }

    public MapScene civilians(LinkedList<Position> value) {
        civilians = value;
        return this;
    }

    public MapScene incidents(LinkedList<Incident> value) {
        incidents = value;
        return this;
    }

    public MapScene operator(Position value) {
        operator = value;
        return this;
    }

    public MapScene selected(Position value) {
        selected = value;
        return this;
    }

    public MapScene pendingBlock(Position value) {
        pendingBlock = value;
        return this;
    }

    public MapScene highlightedVehicle(String value) {
        highlightedVehicle = value;
        return this;
    }
}
