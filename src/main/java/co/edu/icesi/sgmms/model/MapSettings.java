package co.edu.icesi.sgmms.model;

import java.io.Serializable;

/**
 * Numeric parameters of the road network, read from the configuration file.
 *
 * <p>Base weight of a road segment between two cells = {@code baseDistance} × the average of the
 * cost factors of both cells. Main roads are the fastest (factor 1.0); residential and commercial
 * streets are slower because of pedestrians and parked cars. Current weight = base weight ×
 * (1 + {@code congestionStep} × vehicles in the busiest of the two districts).
 */
public class MapSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private double baseDistance = 1.0;
    private double congestionStep = 0.25;
    private int zoneCapacity = 4;
    private int districtSize = 6;
    private double mainRoadFactor = 1.0;
    private double pathFactor = 1.5;
    private double residentialFactor = 2.0;
    private double commercialFactor = 2.5;

    /** Cost factor of a cell type (obstacles never take part in a segment). Complexity — O(1). */
    public double factor(ZoneType zone) {
        switch (zone) {
            case MAIN_ROAD:
                return mainRoadFactor;
            case RESIDENTIAL:
                return residentialFactor;
            case COMMERCIAL:
                return commercialFactor;
            case AVAILABLE_PATH:
                return pathFactor;
            default:
                return Double.POSITIVE_INFINITY;
        }
    }

    /** Complexity — time O(1), space O(1). */
    public double getBaseDistance() {
        return baseDistance;
    }

    public void setBaseDistance(double baseDistance) {
        this.baseDistance = baseDistance;
    }

    /** Complexity — time O(1), space O(1). */
    public double getCongestionStep() {
        return congestionStep;
    }

    public void setCongestionStep(double congestionStep) {
        this.congestionStep = congestionStep;
    }

    /** Maximum vehicles in one district before "critical congestion" (accident criterion 2). */
    public int getZoneCapacity() {
        return zoneCapacity;
    }

    public void setZoneCapacity(int zoneCapacity) {
        this.zoneCapacity = zoneCapacity;
    }

    /** Side, in cells, of the square districts used to count congestion. */
    public int getDistrictSize() {
        return districtSize;
    }

    public void setDistrictSize(int districtSize) {
        this.districtSize = districtSize;
    }

    public void setFactors(double mainRoad, double path, double residential, double commercial) {
        this.mainRoadFactor = mainRoad;
        this.pathFactor = path;
        this.residentialFactor = residential;
        this.commercialFactor = commercial;
    }
}
