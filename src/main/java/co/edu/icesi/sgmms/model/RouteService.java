package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.structures.AllPairsResult;
import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.MstResult;
import co.edu.icesi.sgmms.model.structures.ShortestPathResult;

/**
 * Routing information for dispatch.
 *
 * <p><b>Dijkstra on demand vs. Floyd-Warshall precomputed (RF32, RF33).</b> Dijkstra runs on the
 * traffic graph every time a vehicle is assigned, so the route reflects the congestion of that
 * moment, at O((V + E) log V) per query. Floyd-Warshall runs once on the base graph (Θ(V³)) and is
 * recomputed only when the topology changes (a road is blocked or reopened); afterwards any
 * distance is read in O(1). The matrix answers the structural questions — response time per zone,
 * candidate ranking, "how far from optimal was this choice?" — and Dijkstra answers the
 * operational one — "which streets does this ambulance take now?".
 *
 * <p>Not serialized: it is a cache derived from the map and is rebuilt after loading.
 */
public class RouteService {

    private final CityMap map;
    private AllPairsResult<Position> matrix;
    private int matrixVersion = -1;
    private HashTable<ZoneType, Double> responseByZone;
    private MstResult<Position> prim;
    private MstResult<Position> kruskal;
    private int corridorVersion = -1;
    private long lastFloydWarshallMillis;

    public RouteService(CityMap map) {
        this.map = map;
    }

    /**
     * Dijkstra from {@code source} on the current traffic weights.
     *
     * <p>Complexity — time: O((V + E) log V); space: O(V + E).
     */
    public ShortestPathResult<Position> routesFrom(Position source) {
        return map.trafficGraph().dijkstra(source);
    }

    /**
     * Floyd-Warshall matrix, recomputed only if the topology changed since the last call.
     *
     * <p>Complexity — time: Θ(V³) when recomputed, O(1) otherwise; space: Θ(V²).
     */
    public AllPairsResult<Position> distanceMatrix() {
        if (matrix == null || matrixVersion != map.topologyVersion()) {
            long start = System.nanoTime();
            matrix = map.matrixGraph().floydWarshall();
            lastFloydWarshallMillis = (System.nanoTime() - start) / 1_000_000;
            matrixVersion = map.topologyVersion();
            responseByZone = computeResponseTimes();
        }
        return matrix;
    }

    /**
     * Structural distance between two cells (base weights, no congestion).
     *
     * <p>Complexity — time: O(1) once the matrix is up to date; space: O(1).
     */
    public double structuralDistance(Position a, Position b) {
        return distanceMatrix().distance(a, b);
    }

    /**
     * Estimated response time (simulated minutes) from the monitoring center to a kind of zone:
     * the average Floyd-Warshall distance to its cells, precomputed with the matrix.
     *
     * <p>Complexity — time: O(1) per query (one hash lookup) once the matrix is up to date.
     */
    public double responseTime(ZoneType zone) {
        distanceMatrix();
        Double value = responseByZone.get(zone);
        return value == null ? Double.NaN : value;
    }

    /**
     * Emergency corridors computed with Prim on base distances (cached per topology version).
     *
     * <p>Complexity — time: O(E log V) when recomputed, O(1) otherwise; space: O(V + E).
     */
    public MstResult<Position> primCorridors() {
        refreshCorridors();
        return prim;
    }

    /**
     * Emergency corridors computed with Kruskal on base distances (cached per topology version).
     *
     * <p>Complexity — time: O(E log E) when recomputed, O(1) otherwise; space: O(V + E).
     */
    public MstResult<Position> kruskalCorridors() {
        refreshCorridors();
        return kruskal;
    }

    /** Duration of the last Floyd-Warshall run in milliseconds (shown in the interface). */
    public long getLastFloydWarshallMillis() {
        return lastFloydWarshallMillis;
    }

    private void refreshCorridors() {
        if (prim == null || corridorVersion != map.topologyVersion()) {
            prim = map.baseGraph().primMST(map.getMonitoringCenter());
            kruskal = map.baseGraph().kruskalMST();
            corridorVersion = map.topologyVersion();
        }
    }

    private HashTable<ZoneType, Double> computeResponseTimes() {
        HashTable<ZoneType, Double> result = new HashTable<>();
        Position center = map.getMonitoringCenter();
        for (ZoneType zone : new ZoneType[] {ZoneType.RESIDENTIAL, ZoneType.COMMERCIAL, ZoneType.MAIN_ROAD}) {
            double sum = 0;
            int count = 0;
            for (Position p : map.cellsOf(zone)) {
                double d = matrix.distance(center, p);
                if (d != Double.POSITIVE_INFINITY) {
                    sum += d;
                    count++;
                }
            }
            result.put(zone, count == 0 ? Double.NaN : sum / count);
        }
        return result;
    }
}
