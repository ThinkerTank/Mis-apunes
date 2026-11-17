package co.edu.icesi.sgmms.model.exceptions;

/**
 * Base type of every domain and data-structure error raised by the SGMMS model.
 *
 * <p>It is unchecked on purpose: these errors describe invalid operations requested by the
 * operator (an empty structure, an incompatible vehicle, a blocked move), so they are caught once,
 * at the controller level, and turned into a clear message without closing the application.
 * Errors caused by the file system are checked instead (see {@code PersistenceException}).
 */
public class SgmmsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SgmmsException(String message) {
        super(message);
    }

    public SgmmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
