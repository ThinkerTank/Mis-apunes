package co.edu.icesi.sgmms.model;

/** The four movements of the operator: W/↑ up, A/← left, S/↓ down, D/→ right. */
public enum Direction {
    UP(0, -1, "Arriba"),
    DOWN(0, 1, "Abajo"),
    LEFT(-1, 0, "Izquierda"),
    RIGHT(1, 0, "Derecha");

    private final int dx;
    private final int dy;
    private final String label;

    Direction(int dx, int dy, String label) {
        this.dx = dx;
        this.dy = dy;
        this.label = label;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    /** Text shown to the operator. */
    public String getLabel() {
        return label;
    }
}
