package co.edu.icesi.sgmms.model.exceptions;

/** Raised when the operator tries to leave the map or step onto an obstacle. */
public class InvalidMovementException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public InvalidMovementException(String message) {
        super(message);
    }
}
