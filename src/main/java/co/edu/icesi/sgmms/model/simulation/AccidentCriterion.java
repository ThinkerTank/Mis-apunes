package co.edu.icesi.sgmms.model.simulation;

import co.edu.icesi.sgmms.model.Severity;

/**
 * The three criteria that turn the behaviour of moving vehicles into a traffic accident. Accidents
 * never appear at random: each one is the consequence of one of these situations.
 */
public enum AccidentCriterion {

    /**
     * Two vehicles try to occupy the same cell in the same minute. It is the most direct cause of a
     * crash: neither yielded the way. Medium severity: urban speeds, two vehicles involved.
     */
    COLLISION("Colisión: dos vehículos intentaron ocupar la misma posición", Severity.MEDIUM),

    /**
     * A district holds more vehicles than its capacity ({@code zoneCapacity} in the configuration).
     * Saturated streets bring stop-and-go traffic, lane changes and fender benders. Low severity:
     * low speeds, but it happens repeatedly while the jam lasts.
     */
    CRITICAL_CONGESTION("Congestión crítica: la zona superó su capacidad máxima de vehículos", Severity.LOW),

    /**
     * Two or more vehicles coming from different directions converge on a risk point in the same
     * minute. Risk points are intersections found by DFS where cycles of the road network close
     * (several routes cross there) and that are on or next to a main road. High severity: crossing
     * traffic at main-road speed.
     */
    RISK_POINT_CONVERGENCE("Cruce en punto crítico: vehículos de rutas distintas coincidieron en una intersección de riesgo",
            Severity.HIGH);

    private final String description;
    private final Severity severity;

    AccidentCriterion(String description, Severity severity) {
        this.description = description;
        this.severity = severity;
    }

    /** Explanation shown in the incident description. */
    public String getDescription() {
        return description;
    }

    /** Severity assigned to accidents caused by this criterion. */
    public Severity getSeverity() {
        return severity;
    }
}
