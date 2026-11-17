package co.edu.icesi.sgmms.model.exceptions;

/** Raised when the vehicle type cannot attend the incident type (for example, a fire truck sent to a robbery). */
public class IncompatibleVehicleException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public IncompatibleVehicleException(String message) {
        super(message);
    }
}
