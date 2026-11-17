package co.edu.icesi.sgmms.model.exceptions;

/** Raised when a vehicle that is en route, attending or out of service is selected for a new incident. */
public class VehicleNotAvailableException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}
