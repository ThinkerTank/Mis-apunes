package co.edu.icesi.sgmms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.Direction;
import co.edu.icesi.sgmms.model.DispatchCenter;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentType;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Severity;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.simulation.Simulation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimulationRepositoryTest {

    private static Simulation runningSimulation() throws Exception {
        SgmmsConfig config = ConfigLoader.loadDefault();
        config.getSettings().setSeed(99);
        Simulation simulation = config.createSimulation();
        for (int minute = 0; minute < 20; minute++) {
            simulation.clockTick();
            simulation.generationTick();
        }
        return simulation;
    }

    private static String ids(DispatchCenter center) {
        StringBuilder text = new StringBuilder();
        for (Incident incident : center.activeIncidentsByPriority()) {
            text.append(incident.getId()).append(':').append(incident.getStatus()).append(' ');
        }
        return text.toString();
    }

    @Test
    void savedStateIsRestoredCompletelyAndTheSimulationContinues(@TempDir Path folder) throws Exception {
        Simulation simulation = runningSimulation();
        DispatchCenter center = simulation.getCenter();
        Incident fire = center.reportIncident(IncidentType.FIRE, new Position(12, 1), Severity.HIGH, "prueba");
        center.assignVehicle(fire.getId(), center.suggestVehicle(fire.getId()).getId());
        center.blockRoad(new Position(0, 5), new Position(1, 5));
        center.moveOperator(Direction.UP);
        center.moveOperator(Direction.UP);
        Path file = folder.resolve("partida.sgmms");

        SimulationRepository.save(simulation, file);
        Simulation restored = SimulationRepository.load(file);
        DispatchCenter copy = restored.getCenter();

        assertEquals(center.currentMinute(), copy.currentMinute());
        assertEquals(center.getScore(), copy.getScore());
        assertEquals(ids(center), ids(copy));
        assertEquals(center.nextInDispatchQueue() == null ? null : center.nextInDispatchQueue().getId(),
                copy.nextInDispatchQueue() == null ? null : copy.nextInDispatchQueue().getId());
        for (Vehicle vehicle : center.vehicleList()) {
            Vehicle twin = copy.findVehicle(vehicle.getId());
            assertEquals(vehicle.getStatus(), twin.getStatus());
            assertEquals(vehicle.getPosition(), twin.getPosition());
        }
        assertTrue(copy.getMap().isBlocked(new Position(0, 5), new Position(1, 5)));
        assertEquals(center.operatorPosition(), copy.operatorPosition());
        assertEquals(2, copy.operatorHistorySize());
        assertEquals(center.lastAction().getType(), copy.lastAction().getType());
        assertTrue(copy.isConsistent());

        copy.undoOperatorMovement();
        restored.clockTick();
        assertEquals(center.currentMinute() + 1, copy.currentMinute());
        assertEquals(simulation.civilianPositions().size(), restored.civilianPositions().size());
        Incident robbery = copy.reportIncident(IncidentType.ROBBERY, new Position(9, 1), Severity.LOW, "después");
        assertNotNull(copy.suggestVehicle(robbery.getId()));
    }

    @Test
    void missingFileIsReported(@TempDir Path folder) {
        PersistenceException error = assertThrows(PersistenceException.class,
                () -> SimulationRepository.load(folder.resolve("no-existe.sgmms")));
        assertTrue(error.getMessage().contains("No existe"));
    }

    @Test
    void aFileThatIsNotASaveIsRejected(@TempDir Path folder) throws Exception {
        Path json = folder.resolve("city.json");
        Files.writeString(json, "{\"grid\": []}");

        PersistenceException error = assertThrows(PersistenceException.class, () -> SimulationRepository.load(json));
        assertTrue(error.getMessage().contains("no es una simulación"), error.getMessage());
    }

    @Test
    void aTruncatedSaveIsReportedInsteadOfCrashing(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("partida.sgmms");
        SimulationRepository.save(runningSimulation(), file);
        byte[] bytes = Files.readAllBytes(file);
        byte[] half = new byte[bytes.length / 2];
        System.arraycopy(bytes, 0, half, 0, half.length);
        Files.write(file, half);

        assertThrows(PersistenceException.class, () -> SimulationRepository.load(file));
    }
}
