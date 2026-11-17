package co.edu.icesi.sgmms.persistence;

import co.edu.icesi.sgmms.model.simulation.Simulation;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves and restores the whole simulation with Java object serialization (RF16).
 *
 * <p><b>What goes where.</b> JSON holds the <i>initial configuration</i> that a person writes and
 * edits by hand: the grid, the route parameters and the vehicle fleet. Serialization holds the
 * <i>running state</i>, which nobody edits and which is a graph of interconnected objects: active
 * and resolved incidents in their BST and hash index, the dispatch and attention queues, the
 * action and movement stacks, vehicle states and routes, private traffic, blocked roads, the
 * score, the clock and the random generator. Serializing it keeps every reference intact (the
 * same {@code Incident} object lives in four structures) without writing a format by hand.
 *
 * <p>Derived caches are not written: the road graphs are rebuilt from the grid and the blocked
 * roads, and the Floyd-Warshall matrix is recomputed on first use.
 */
public final class SimulationRepository {

    /** Marker written before the object so other files are recognised and rejected. */
    static final String HEADER = "SGMMS-SAVE-1";

    private SimulationRepository() {
    }

    /**
     * Writes the simulation to {@code file}. The simulation and its dispatch center are locked
     * while writing (in the same order the simulation threads use), so the snapshot is consistent
     * even if the clock is running. The file is written to a temporary file first and then moved,
     * so a failure never leaves a half-written save.
     *
     * <p>Complexity — time: O(S) for a state of size S; space: O(S) in the output stream.
     *
     * @throws PersistenceException if the file cannot be written
     */
    public static void save(Simulation simulation, Path file) throws PersistenceException {
        if (file == null) {
            throw new PersistenceException("Debe indicar un archivo para guardar la simulación");
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            synchronized (simulation) {
                synchronized (simulation.getCenter()) {
                    try (ObjectOutputStream out = new ObjectOutputStream(
                            new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                        out.writeUTF(HEADER);
                        out.writeObject(simulation);
                    }
                }
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // nothing else can be done; the original error is reported below
            }
            throw new PersistenceException("No se pudo guardar la simulación en " + file + ": " + error.getMessage(), error);
        }
    }

    /**
     * Reads a simulation saved with {@link #save}.
     *
     * <p>Complexity — time: O(S) plus O(V²) to rebuild the adjacency matrix; space: O(S + V²).
     *
     * @throws PersistenceException if the file is missing, not an SGMMS save, corrupt or from an
     *                              incompatible version
     */
    public static Simulation load(Path file) throws PersistenceException {
        if (file == null || !Files.exists(file)) {
            throw new PersistenceException("No existe el archivo de simulación: " + file);
        }
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            String header;
            try {
                header = in.readUTF();
            } catch (IOException notOurs) {
                throw new PersistenceException("El archivo " + file.getFileName() + " no es una simulación guardada del SGMMS");
            }
            if (!HEADER.equals(header)) {
                throw new PersistenceException("El archivo " + file.getFileName() + " no es una simulación guardada del SGMMS");
            }
            Object state = in.readObject();
            if (!(state instanceof Simulation)) {
                throw new PersistenceException("El archivo " + file.getFileName() + " no contiene una simulación");
            }
            return (Simulation) state;
        } catch (StreamCorruptedException corrupt) {
            throw new PersistenceException("El archivo " + file.getFileName() + " no es una simulación guardada del SGMMS", corrupt);
        } catch (InvalidClassException version) {
            throw new PersistenceException("La simulación fue guardada con una versión incompatible del programa", version);
        } catch (ClassNotFoundException missing) {
            throw new PersistenceException("El archivo hace referencia a clases que no existen en esta versión", missing);
        } catch (NoSuchFileException gone) {
            throw new PersistenceException("No existe el archivo de simulación: " + file, gone);
        } catch (IOException error) {
            throw new PersistenceException("No se pudo leer la simulación: " + error.getMessage(), error);
        }
    }
}
