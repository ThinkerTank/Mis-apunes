package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.util.Comparator;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PriorityQueueTest {

    /** Task with a priority level and an arrival time, to check the tie-break by age. */
    private static final class Task {
        final String name;
        final int level;
        final long arrival;

        Task(String name, int level, long arrival) {
            this.name = name;
            this.level = level;
            this.arrival = arrival;
        }
    }

    /** Higher level first; on a tie, the older task (smaller arrival) first. */
    private static final Comparator<Task> BY_LEVEL_THEN_AGE =
            Comparator.<Task>comparingInt(t -> t.level).thenComparing(t -> -t.arrival);

    @Test
    void extractMaxReturnsElementsByPriority() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.naturalOrder());
        int[] values = {5, 1, 9, 3, 7};
        for (int value : values) {
            queue.insert(value);
        }

        assertEquals(9, queue.extractMax());
        assertEquals(7, queue.extractMax());
        assertEquals(5, queue.extractMax());
        assertEquals(2, queue.size());
    }

    @Test
    void tieOnPriorityIsBrokenByAge() {
        PriorityQueue<Task> queue = new PriorityQueue<>(BY_LEVEL_THEN_AGE);
        queue.insert(new Task("newer-high", 3, 200));
        queue.insert(new Task("low", 1, 50));
        queue.insert(new Task("older-high", 3, 100));

        assertEquals("older-high", queue.extractMax().name);
        assertEquals("newer-high", queue.extractMax().name);
        assertEquals("low", queue.extractMax().name);
    }

    @Test
    void peekMaxDoesNotRemoveAndEmptyQueueThrows() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.naturalOrder());
        assertThrows(EmptyStructureException.class, queue::peekMax);
        assertThrows(EmptyStructureException.class, queue::extractMax);

        queue.insert(4);
        queue.insert(8);
        assertEquals(8, queue.peekMax());
        assertEquals(2, queue.size());
    }

    @Test
    void reversedComparatorBehavesAsMinHeap() {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(Comparator.<Integer>naturalOrder().reversed());
        minHeap.insert(5);
        minHeap.insert(2);
        minHeap.insert(8);

        assertEquals(2, minHeap.extractMax());
        assertEquals(5, minHeap.extractMax());
    }

    @Test
    void removeArbitraryElementKeepsHeapOrder() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.naturalOrder());
        for (int value = 1; value <= 10; value++) {
            queue.insert(value);
        }

        assertTrue(queue.remove(7));
        assertFalse(queue.remove(70));
        assertFalse(queue.contains(7));

        int previous = Integer.MAX_VALUE;
        while (!queue.isEmpty()) {
            int current = queue.extractMax();
            assertTrue(current < previous);
            previous = current;
        }
    }

    @Test
    void manyRandomInsertionsComeOutSorted() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.naturalOrder());
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            queue.insert(random.nextInt(1000));
        }

        int previous = Integer.MAX_VALUE;
        while (!queue.isEmpty()) {
            int current = queue.extractMax();
            assertTrue(current <= previous);
            previous = current;
        }
    }
}
