package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.InvalidMapException;
import co.edu.icesi.sgmms.model.structures.AdjacencyListGraph;
import co.edu.icesi.sgmms.model.structures.AdjacencyMatrixGraph;
import co.edu.icesi.sgmms.model.structures.BfsResult;
import co.edu.icesi.sgmms.model.structures.DfsResult;
import co.edu.icesi.sgmms.model.structures.Edge;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.IGraph;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * The city of Palmira as a grid of cells and, at the same time, as a weighted graph (RF29).
 *
 * <p><b>Dual representation.</b> The grid ({@code int[][]} of zone codes) is what the traffic map
 * draws. Every traversable cell is a vertex and every pair of orthogonally adjacent traversable
 * cells is joined by an undirected road segment. The graph is kept in three forms:
 * <ul>
 *   <li>{@code baseRoads} — adjacency lists with base distances (no congestion). Used for the
 *       BFS connectivity check, DFS analysis and the emergency corridors (MST).</li>
 *   <li>{@code trafficRoads} — adjacency lists with the current weights
 *       {@code base × congestion factor}. Dijkstra routes vehicles on it.</li>
 *   <li>{@code matrixRoads} — adjacency matrix with base distances, input of Floyd-Warshall.</li>
 * </ul>
 * The grid has at most four neighbors per cell, so E ≤ 2V: the lists use Θ(V + E) = Θ(V) memory,
 * while the matrix uses Θ(V²) even though almost all its cells are empty.
 *
 * <p>The graphs are not serialized: they are rebuilt from the grid and the list of blocked roads
 * when a saved simulation is loaded.
 */
public class CityMap implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int[][] cells;
    private final int width;
    private final int height;
    private final Position monitoringCenter;
    private final MapSettings settings;
    private final LinkedList<Edge<Position>> blocked = new LinkedList<>();
    private int[] congestion;
    private int topologyVersion;

    private transient AdjacencyListGraph<Position> baseRoads;
    private transient AdjacencyListGraph<Position> trafficRoads;
    private transient AdjacencyMatrixGraph<Position> matrixRoads;
    private transient int riskVersion;
    private transient HashTable<Position, Boolean> riskLookup;
    private transient LinkedList<Position> riskPoints;

    /**
     * Builds the map and validates it: rectangular grid, valid codes, monitoring center on a
     * traversable cell, and every zone reachable from the center (BFS, RF30).
     *
     * <p>Complexity — time: O(V²) because the adjacency matrix is allocated (BFS and the lists are
     * O(V + E)); space: O(V²).
     *
     * @throws InvalidMapException if any of those checks fails
     */
    public CityMap(int[][] grid, Position monitoringCenter, MapSettings settings) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            throw new InvalidMapException("La cuadrícula del mapa está vacía");
        }
        this.height = grid.length;
        this.width = grid[0].length;
        this.cells = new int[height][width];
        for (int y = 0; y < height; y++) {
            if (grid[y].length != width) {
                throw new InvalidMapException("La fila " + y + " tiene " + grid[y].length
                        + " celdas pero la fila 0 tiene " + width + "; la cuadrícula debe ser rectangular");
            }
            for (int x = 0; x < width; x++) {
                cells[y][x] = ZoneType.fromCode(grid[y][x]).getCode();
            }
        }
        this.settings = settings;
        validateSettings(settings);
        if (!isInside(monitoringCenter) || !canEnter(monitoringCenter)) {
            throw new InvalidMapException("El centro de monitoreo " + monitoringCenter
                    + " debe estar en una celda transitable dentro del mapa");
        }
        this.monitoringCenter = monitoringCenter;
        this.congestion = new int[districtCount()];
        buildGraphs();
        validateConnectivity();
    }

    // ---------------------------------------------------------------- grid queries

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Position getMonitoringCenter() {
        return monitoringCenter;
    }

    public MapSettings getSettings() {
        return settings;
    }

    /** Whether the position lies inside the grid. Complexity — O(1). */
    public boolean isInside(Position p) {
        return p != null && p.getX() >= 0 && p.getY() >= 0 && p.getX() < width && p.getY() < height;
    }

    /** Zone of a cell. Complexity — O(1). */
    public ZoneType zoneAt(Position p) {
        if (!isInside(p)) {
            throw new IllegalArgumentException(p + " está fuera del mapa");
        }
        return ZoneType.fromCode(cells[p.getY()][p.getX()]);
    }

    /** Zone of a cell by coordinates. Complexity — O(1). */
    public ZoneType zoneAt(int x, int y) {
        return zoneAt(new Position(x, y));
    }

    /** Whether vehicles and the operator may stand on the cell. Complexity — O(1). */
    public boolean canEnter(Position p) {
        return isInside(p) && zoneAt(p).isTraversable();
    }

    /** Every cell of one zone type, row by row. Complexity — O(W·H). */
    public LinkedList<Position> cellsOf(ZoneType zone) {
        LinkedList<Position> result = new LinkedList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cells[y][x] == zone.getCode()) {
                    result.addLast(new Position(x, y));
                }
            }
        }
        return result;
    }

    /** Every traversable cell (the vertices of the graph). Complexity — O(V). */
    public LinkedList<Position> traversableCells() {
        return baseRoads.vertices();
    }

    // ---------------------------------------------------------------- graphs

    /** Road graph with current (congested) weights, used by Dijkstra. */
    public IGraph<Position> trafficGraph() {
        return trafficRoads;
    }

    /** Road graph with base distances, used by BFS, DFS, Prim and Kruskal. */
    public IGraph<Position> baseGraph() {
        return baseRoads;
    }

    /** Adjacency-matrix copy of the base graph, used by Floyd-Warshall. */
    public IGraph<Position> matrixGraph() {
        return matrixRoads;
    }

    /** Open neighbors of a cell (blocked segments excluded). Complexity — O(deg) ≤ O(4). */
    public LinkedList<Position> openNeighbors(Position p) {
        return trafficRoads.getAdjacent(p);
    }

    /**
     * Base distance of the segment between two adjacent traversable cells.
     *
     * <p>Complexity — time and space O(1).
     */
    public double baseWeight(Position a, Position b) {
        return settings.getBaseDistance() * (settings.factor(zoneAt(a)) + settings.factor(zoneAt(b))) / 2.0;
    }

    /** Current weight (with congestion) or infinity if the segment is closed. Complexity — O(4). */
    public double currentWeight(Position a, Position b) {
        return trafficRoads.getWeight(a, b);
    }

    /** Incremented every time a road is blocked or unblocked. Complexity — O(1). */
    public int topologyVersion() {
        return topologyVersion;
    }

    // ---------------------------------------------------------------- districts and congestion

    /** District (square block of cells) that contains the position. Complexity — O(1). */
    public int districtOf(Position p) {
        int perRow = (width + settings.getDistrictSize() - 1) / settings.getDistrictSize();
        return (p.getY() / settings.getDistrictSize()) * perRow + p.getX() / settings.getDistrictSize();
    }

    /** Number of districts. Complexity — O(1). */
    public int districtCount() {
        int size = settings.getDistrictSize();
        return ((width + size - 1) / size) * ((height + size - 1) / size);
    }

    /** Vehicles counted in a district at the last congestion update. Complexity — O(1). */
    public int vehiclesInDistrict(int district) {
        return congestion[district];
    }

    /**
     * Recomputes the current weight of every open segment from the number of vehicles per
     * district: {@code w = base × (1 + step × max(vehicles in either district))}.
     *
     * <p>Complexity — time: O(V + E); space: O(E) for the temporary edge list.
     */
    public void applyCongestion(int[] vehiclesPerDistrict) {
        if (vehiclesPerDistrict.length != congestion.length) {
            throw new IllegalArgumentException("Se esperaban " + congestion.length + " conteos de distrito");
        }
        congestion = vehiclesPerDistrict.clone();
        for (Edge<Position> edge : baseRoads.edges()) {
            trafficRoads.updateWeight(edge.getFrom(), edge.getTo(), congestedWeight(edge.getFrom(), edge.getTo()));
        }
    }

    private double congestedWeight(Position a, Position b) {
        int vehicles = Math.max(congestion[districtOf(a)], congestion[districtOf(b)]);
        return baseWeight(a, b) * (1 + settings.getCongestionStep() * vehicles);
    }

    // ---------------------------------------------------------------- blocked roads

    /**
     * Closes the segment between two adjacent cells in the three graphs.
     *
     * <p>Complexity — time: O(1) on the matrix and O(deg) ≤ O(4) on each list; space: O(1).
     *
     * @throws InvalidMapException if the cells are not adjacent, not traversable or already blocked
     */
    public void blockRoad(Position a, Position b) {
        requireSegment(a, b);
        if (isBlocked(a, b)) {
            throw new InvalidMapException("La vía " + a + " - " + b + " ya está bloqueada");
        }
        baseRoads.removeEdge(a, b);
        trafficRoads.removeEdge(a, b);
        matrixRoads.removeEdge(a, b);
        blocked.addLast(new Edge<>(a, b, baseWeight(a, b)));
        topologyVersion++;
    }

    /**
     * Reopens a blocked segment.
     *
     * <p>Complexity — time: O(B) to find it among B blocked roads, plus O(4); space: O(1).
     *
     * @throws InvalidMapException if the segment is not blocked
     */
    public void unblockRoad(Position a, Position b) {
        requireSegment(a, b);
        Edge<Position> edge = findBlocked(a, b);
        if (edge == null) {
            throw new InvalidMapException("La vía " + a + " - " + b + " no está bloqueada");
        }
        blocked.remove(edge);
        baseRoads.addEdge(a, b, baseWeight(a, b));
        matrixRoads.addEdge(a, b, baseWeight(a, b));
        trafficRoads.addEdge(a, b, congestedWeight(a, b));
        topologyVersion++;
    }

    /** Whether the segment a–b is blocked. Complexity — O(B). */
    public boolean isBlocked(Position a, Position b) {
        return findBlocked(a, b) != null;
    }

    /** Copy of the list of blocked segments. Complexity — O(B). */
    public LinkedList<Edge<Position>> blockedRoads() {
        return blocked.copy();
    }

    // ---------------------------------------------------------------- analysis

    /**
     * BFS from the monitoring center over the open roads (RF30).
     *
     * <p>Complexity — time: O(V + E); space: O(V).
     *
     * @throws InvalidMapException listing the zone cells that cannot be reached
     */
    public BfsResult<Position> validateConnectivity() {
        BfsResult<Position> bfs = baseRoads.bfs(monitoringCenter);
        LinkedList<Position> disconnected = bfs.unreachable().filter(p -> zoneAt(p).isZone());
        if (!disconnected.isEmpty()) {
            StringBuilder message = new StringBuilder("Zonas inalcanzables desde el centro de monitoreo: ");
            int shown = 0;
            for (Position p : disconnected) {
                if (shown++ == 8) {
                    message.append("... (").append(disconnected.size()).append(" celdas en total)");
                    break;
                }
                message.append(p).append(' ').append(zoneAt(p).getLabel()).append("; ");
            }
            throw new InvalidMapException(message.toString().trim());
        }
        return bfs;
    }

    /**
     * Coverage of the monitoring center: average and maximum BFS hops to each kind of zone.
     *
     * <p>Complexity — time: O(V + E); space: O(V).
     */
    public LinkedList<ZoneCoverage> coverage() {
        BfsResult<Position> bfs = baseRoads.bfs(monitoringCenter);
        LinkedList<ZoneCoverage> result = new LinkedList<>();
        for (ZoneType zone : new ZoneType[] {ZoneType.RESIDENTIAL, ZoneType.COMMERCIAL, ZoneType.MAIN_ROAD}) {
            int count = 0;
            int max = 0;
            long sum = 0;
            for (Position p : cellsOf(zone)) {
                int hops = bfs.hopsTo(p);
                if (hops >= 0) {
                    count++;
                    sum += hops;
                    max = Math.max(max, hops);
                }
            }
            result.addLast(new ZoneCoverage(zone, count, count == 0 ? 0 : (double) sum / count, max));
        }
        return result;
    }

    /**
     * DFS over the open roads: connected components (they appear when blocked roads split the
     * city) and the vertices where cycles close (RF31).
     *
     * <p>Complexity — time: O(V + E); space: O(V).
     */
    public DfsResult<Position> structuralAnalysis() {
        return baseRoads.dfs();
    }

    /**
     * Risk points: intersections where vehicle routes converge. Traffic concentrates on the main
     * roads, so the analysis runs DFS on the subgraph of open main-road segments: a vertex is a
     * risk point when it closes a cycle of that network (alternative routes meet there) and at
     * least three main-road segments leave it (a real crossing, not a curve). Recomputed only when
     * the topology changes.
     *
     * <p>Complexity — time: O(V + E) when recomputed, O(1) otherwise; space: O(V).
     */
    public LinkedList<Position> riskPoints() {
        refreshRiskPoints();
        return riskPoints;
    }

    /** Whether the cell is a risk point. Complexity — average O(1) after the first call. */
    public boolean isRiskPoint(Position p) {
        refreshRiskPoints();
        return riskLookup.containsKey(p);
    }

    // ---------------------------------------------------------------- internals

    private void refreshRiskPoints() {
        if (riskPoints != null && riskVersion == topologyVersion) {
            return;
        }
        AdjacencyListGraph<Position> mainNetwork = new AdjacencyListGraph<>();
        for (Position p : cellsOf(ZoneType.MAIN_ROAD)) {
            mainNetwork.addVertex(p);
        }
        for (Edge<Position> edge : baseRoads.edges()) {
            if (zoneAt(edge.getFrom()) == ZoneType.MAIN_ROAD && zoneAt(edge.getTo()) == ZoneType.MAIN_ROAD) {
                mainNetwork.addEdge(edge.getFrom(), edge.getTo(), edge.getWeight());
            }
        }
        riskPoints = new LinkedList<>();
        riskLookup = new HashTable<>();
        for (Position p : mainNetwork.dfs().cycleVertices()) {
            if (mainNetwork.getAdjacent(p).size() >= 3) {
                riskPoints.addLast(p);
                riskLookup.put(p, Boolean.TRUE);
            }
        }
        riskVersion = topologyVersion;
    }

    private void buildGraphs() {
        baseRoads = new AdjacencyListGraph<>();
        trafficRoads = new AdjacencyListGraph<>();
        matrixRoads = new AdjacencyMatrixGraph<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position p = new Position(x, y);
                if (canEnter(p)) {
                    baseRoads.addVertex(p);
                    trafficRoads.addVertex(p);
                    matrixRoads.addVertex(p);
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position p = new Position(x, y);
                if (!canEnter(p)) {
                    continue;
                }
                for (Position q : new Position[] {new Position(x + 1, y), new Position(x, y + 1)}) {
                    if (canEnter(q)) {
                        double weight = baseWeight(p, q);
                        baseRoads.addEdge(p, q, weight);
                        trafficRoads.addEdge(p, q, weight);
                        matrixRoads.addEdge(p, q, weight);
                    }
                }
            }
        }
        for (Edge<Position> edge : blocked) {
            baseRoads.removeEdge(edge.getFrom(), edge.getTo());
            trafficRoads.removeEdge(edge.getFrom(), edge.getTo());
            matrixRoads.removeEdge(edge.getFrom(), edge.getTo());
        }
        applyCongestion(congestion);
        riskPoints = null;
    }

    private void requireSegment(Position a, Position b) {
        if (!canEnter(a) || !canEnter(b) || !a.isAdjacentTo(b)) {
            throw new InvalidMapException("No hay un tramo de vía entre " + a + " y " + b);
        }
    }

    private Edge<Position> findBlocked(Position a, Position b) {
        return blocked.find(e -> (e.getFrom().equals(a) && e.getTo().equals(b))
                || (e.getFrom().equals(b) && e.getTo().equals(a)));
    }

    private static void validateSettings(MapSettings s) {
        if (s == null) {
            throw new InvalidMapException("Faltan los parámetros del mapa");
        }
        if (s.getBaseDistance() <= 0 || s.getCongestionStep() < 0 || s.getZoneCapacity() < 1 || s.getDistrictSize() < 1) {
            throw new InvalidMapException("Los parámetros del mapa deben ser positivos (distancia base > 0, paso de congestión >= 0, "
                    + "capacidad de zona >= 1, tamaño de distrito >= 1)");
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        buildGraphs();
    }
}
