package co.edu.icesi.sgmms.model.exceptions;

/** Raised when a vehicle ID is not in the vehicle registry. */
public class VehicleNotFoundException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public VehicleNotFoundException(String message) {
        super(message);
    }
}
