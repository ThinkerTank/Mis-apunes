package co.edu.icesi.sgmms.model.exceptions;

/** Raised when an incident ID is not in the index of active incidents. */
public class IncidentNotFoundException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public IncidentNotFoundException(String message) {
        super(message);
    }
}
