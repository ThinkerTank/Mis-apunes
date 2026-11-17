package co.edu.icesi.sgmms.model.exceptions;

/** Raised when an operation does not fit the incident's state: assigning a vehicle to an incident that is resolved or already in progress, or resolving one whose vehicle has not arrived. */
public class InvalidIncidentStateException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public InvalidIncidentStateException(String message) {
        super(message);
    }
}
