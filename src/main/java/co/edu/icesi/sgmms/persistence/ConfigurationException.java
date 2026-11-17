package co.edu.icesi.sgmms.persistence;

/**
 * The initial configuration cannot be used: the JSON file does not exist or cannot be read, it is
 * malformed, a required field is missing or has the wrong type, or its content is inconsistent
 * (non-rectangular grid, unreachable zones, duplicate vehicle IDs, a vehicle on an obstacle...).
 *
 * <p>It is checked because it comes from the file system and the user can recover from it by
 * choosing another file, so the compiler forces the caller to handle it.
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
