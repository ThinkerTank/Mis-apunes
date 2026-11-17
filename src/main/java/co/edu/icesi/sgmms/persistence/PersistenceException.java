package co.edu.icesi.sgmms.persistence;

/**
 * A saved simulation cannot be written or recovered: the file does not exist, it cannot be
 * written, it is not an SGMMS save, it is corrupt, or it was produced by an incompatible version
 * of the classes. Checked, like {@link ConfigurationException}, because it comes from the file
 * system and the operator can recover by choosing another file.
 */
public class PersistenceException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
