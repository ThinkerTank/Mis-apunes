package co.edu.icesi.sgmms.model;

import co.edu.icesi.sgmms.model.exceptions.InvalidMapException;

/**
 * Kind of cell in the city grid, with the numeric codes used in the configuration file:
 * 0 available path, 1 obstacle, 2 residential zone, 3 commercial zone, 4 main road.
 */
public enum ZoneType {
    AVAILABLE_PATH(0, "Camino disponible"),
    OBSTACLE(1, "Obstáculo"),
    RESIDENTIAL(2, "Zona residencial"),
    COMMERCIAL(3, "Zona comercial"),
    MAIN_ROAD(4, "Vía principal");

    private final int code;
    private final String label;

    ZoneType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** Whether vehicles and the operator can move through cells of this kind. */
    public boolean isTraversable() {
        return this != OBSTACLE;
    }

    /** Whether this is one of the three zones that must be reachable from the monitoring center. */
    public boolean isZone() {
        return this == RESIDENTIAL || this == COMMERCIAL || this == MAIN_ROAD;
    }

    /**
     * Translates a configuration code.
     *
     * @throws InvalidMapException if the code is not between 0 and 4
     */
    public static ZoneType fromCode(int code) {
        for (ZoneType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new InvalidMapException("Código de celda desconocido " + code + " (los códigos válidos van de 0 a 4)");
    }
}
