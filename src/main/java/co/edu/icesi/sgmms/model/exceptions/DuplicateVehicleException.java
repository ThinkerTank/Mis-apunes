package co.edu.icesi.sgmms.model.exceptions;

/** Raised when a vehicle is registered with an ID that already exists. */
public class DuplicateVehicleException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public DuplicateVehicleException(String message) {
        super(message);
    }
}
