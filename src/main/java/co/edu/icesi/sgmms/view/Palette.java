package co.edu.icesi.sgmms.view;

import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.VehicleType;
import co.edu.icesi.sgmms.model.ZoneType;
import javafx.scene.paint.Color;

/** Colors shared by the map, the legend and the tables, so every screen reads the same way. */
public final class Palette {

    public static final Color BACKGROUND = Color.web("#0f172a");
    public static final Color GRID_LINE = Color.web("#0f172a", 0.25);
    public static final Color CORRIDOR = Color.web("#06b6d4");
    public static final Color BLOCKED = Color.web("#ef4444");
    public static final Color RISK = Color.web("#f59e0b");
    public static final Color OPERATOR = Color.web("#facc15");
    public static final Color SELECTION = Color.WHITE;
    public static final Color CIVILIAN = Color.web("#475569");
    public static final Color CENTER = Color.web("#f8fafc");

    private Palette() {
    }

    /**
     * Fill color of a cell.
     *
     * <p>Complexity — time O(1), space O(1).
     */
    public static Color zone(ZoneType zone) {
        switch (zone) {
            case OBSTACLE:
                return Color.web("#334155");
            case RESIDENTIAL:
                return Color.web("#bbf7d0");
            case COMMERCIAL:
                return Color.web("#bfdbfe");
            case MAIN_ROAD:
                return Color.web("#94a3b8");
            default:
                return Color.web("#e2e8f0");
        }
    }

    /**
     * Marker color of an incident type.
     *
     * <p>Complexity — time O(1), space O(1).
     */
    public static Color incident(IncidentType type) {
        switch (type) {
            case ACCIDENT:
                return Color.web("#ea580c");
            case ROBBERY:
                return Color.web("#7c3aed");
            default:
                return Color.web("#dc2626");
        }
    }

    /** Color of a severity level. */
    public static Color severity(Severity severity) {
        switch (severity) {
            case HIGH:
                return Color.web("#b91c1c");
            case MEDIUM:
                return Color.web("#d97706");
            default:
                return Color.web("#15803d");
        }
    }

    /** Body color of a vehicle type. */
    public static Color vehicle(VehicleType type) {
        switch (type) {
            case PATROL:
                return Color.web("#1d4ed8");
            case AMBULANCE:
                return Color.web("#0f766e");
            default:
                return Color.web("#9f1239");
        }
    }

    /** One-letter symbol drawn on a vehicle. */
    public static String vehicleLetter(VehicleType type) {
        switch (type) {
            case PATROL:
                return "P";
            case AMBULANCE:
                return "Am";
            default:
                return "B";
        }
    }

    /** One-letter symbol drawn on an incident. */
    public static String incidentLetter(IncidentType type) {
        switch (type) {
            case ACCIDENT:
                return "A";
            case ROBBERY:
                return "R";
            default:
                return "I";
        }
    }

    /** CSS hex string of a color, for styling labels. */
    public static String css(Color color) {
        return String.format("#%02x%02x%02x", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }
}
