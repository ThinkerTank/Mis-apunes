package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueueTest {

    private Queue<String> queue;

    @BeforeEach
    void setUp() {
        queue = new Queue<>();
    }

    @Test
    void dequeueReturnsElementsInArrivalOrder() {
        queue.enqueue("INC-1");
        queue.enqueue("INC-2");
        queue.enqueue("INC-3");

        assertEquals("INC-1", queue.dequeue());
        assertEquals("INC-2", queue.dequeue());
        assertEquals("INC-3", queue.dequeue());
    }

    @Test
    void frontDoesNotRemove() {
        queue.enqueue("INC-1");
        queue.enqueue("INC-2");

        assertEquals("INC-1", queue.front());
        assertEquals(2, queue.size());
    }

    @Test
    void dequeueAndFrontOnEmptyQueueThrow() {
        assertThrows(EmptyStructureException.class, queue::dequeue);
        assertThrows(EmptyStructureException.class, queue::front);
    }

    @Test
    void enqueueAfterEmptyingStillWorks() {
        queue.enqueue("A");
        queue.dequeue();
        queue.enqueue("B");
        queue.enqueue("C");

        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.front());
    }

    @Test
    void removeTakesAnElementOutOfTheMiddle() {
        queue.enqueue("INC-1");
        queue.enqueue("INC-2");
        queue.enqueue("INC-3");

        assertTrue(queue.remove("INC-2"));
        assertFalse(queue.contains("INC-2"));
        assertEquals("INC-1", queue.dequeue());
        assertEquals("INC-3", queue.dequeue());
        assertTrue(queue.isEmpty());
    }
}
