package co.edu.icesi.sgmms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.IncidentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentRepositoryTest {

    private IncidentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new IncidentRepository();
    }

    @Test
    void findByIdUsesTheHashIndex() {
        Incident fire = TestCity.incident("INC-1", IncidentType.FIRE, Severity.HIGH, 0, 1);
        repository.add(fire);

        assertEquals(fire, repository.findById("INC-1"));
        assertTrue(repository.contains("INC-1"));
        assertThrows(IncidentNotFoundException.class, () -> repository.findById("INC-404"));
    }

    @Test
    void mostUrgentPrefersSeverityThenTheOldest() {
        repository.add(TestCity.incident("LOW", IncidentType.ROBBERY, Severity.LOW, 0, 1));
        repository.add(TestCity.incident("HIGH-NEW", IncidentType.FIRE, Severity.HIGH, 10, 2));
        repository.add(TestCity.incident("HIGH-OLD", IncidentType.ACCIDENT, Severity.HIGH, 5, 3));
        repository.add(TestCity.incident("MED", IncidentType.ROBBERY, Severity.MEDIUM, 1, 4));

        assertEquals("HIGH-OLD", repository.mostUrgent().getId());
        String order = "";
        for (Incident incident : repository.activeByPriority()) {
            order += incident.getId() + " ";
        }
        assertEquals("HIGH-OLD HIGH-NEW MED LOW ", order);
    }

    @Test
    void sameSeverityAndMinuteAreOrderedByCreationSequence() {
        repository.add(TestCity.incident("SECOND", IncidentType.FIRE, Severity.MEDIUM, 3, 8));
        repository.add(TestCity.incident("FIRST", IncidentType.FIRE, Severity.MEDIUM, 3, 7));

        assertEquals(2, repository.activeCount());
        assertEquals("FIRST", repository.mostUrgent().getId());
    }

    @Test
    void archiveRemovesFromTreeAndIndexAtTheSameTime() {
        repository.add(TestCity.incident("A", IncidentType.FIRE, Severity.HIGH, 0, 1));
        repository.add(TestCity.incident("B", IncidentType.ROBBERY, Severity.LOW, 0, 2));

        repository.archive("A");

        assertTrue(repository.isConsistent());
        assertFalse(repository.contains("A"));
        assertEquals("B", repository.mostUrgent().getId());
        assertEquals(1, repository.resolvedHistory().size());
        repository.archive("B");
        assertNull(repository.mostUrgent());
        assertTrue(repository.isConsistent());
    }

    @Test
    void duplicateIdsAreRejectedAndCountsByType() {
        repository.add(TestCity.incident("A", IncidentType.FIRE, Severity.HIGH, 0, 1));
        repository.add(TestCity.incident("B", IncidentType.FIRE, Severity.LOW, 0, 2));
        repository.add(TestCity.incident("C", IncidentType.ROBBERY, Severity.LOW, 0, 3));

        assertThrows(IllegalArgumentException.class,
                () -> repository.add(TestCity.incident("A", IncidentType.ROBBERY, Severity.LOW, 9, 9)));
        assertEquals(2, repository.activeCount(IncidentType.FIRE));
        assertEquals(0, repository.activeCount(IncidentType.ACCIDENT));
        assertEquals(2, repository.activeWithSeverity(Severity.LOW).size());
    }
}
