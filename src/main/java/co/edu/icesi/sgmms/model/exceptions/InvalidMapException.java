package co.edu.icesi.sgmms.model.exceptions;

/** Raised when the city map is inconsistent: unknown cell codes, a non-rectangular grid, a monitoring center on an obstacle, unreachable zones, or an invalid road to block. */
public class InvalidMapException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public InvalidMapException(String message) {
        super(message);
    }
}
