package co.edu.icesi.sgmms.model.exceptions;

/** Raised when there is no open road between a vehicle and an incident, usually because blocked roads split the city. */
public class RouteUnavailableException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public RouteUnavailableException(String message) {
        super(message);
    }
}
