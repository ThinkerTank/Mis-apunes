package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkedListTest {

    private LinkedList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new LinkedList<>();
    }

    @Test
    void addFirstAndAddLastKeepOrderAndBothEnds() {
        list.addLast(2);
        list.addFirst(1);
        list.addLast(3);

        assertEquals(3, list.size());
        assertEquals(1, list.getFirst());
        assertEquals(3, list.getLast());
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    void removeFirstOnEmptyListThrows() {
        assertThrows(EmptyStructureException.class, list::removeFirst);
        assertThrows(EmptyStructureException.class, list::getLast);
    }

    @Test
    void removeLastUpdatesTailSoLaterAppendsStillWork() {
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(3, list.removeLast());
        list.add(4);

        assertEquals("[1, 2, 4]", list.toString());
        assertEquals(4, list.getLast());
    }

    @Test
    void removeValueInTheMiddleAndAtTheTail() {
        for (int i = 1; i <= 4; i++) {
            list.add(i);
        }

        assertTrue(list.remove(2));
        assertTrue(list.remove(4));
        assertFalse(list.remove(99));

        assertEquals("[1, 3]", list.toString());
        assertEquals(3, list.getLast());
    }

    @Test
    void insertAndRemoveAtIndexes() {
        list.add(1);
        list.add(3);
        list.insert(1, 2);
        list.insert(3, 4);

        assertEquals("[1, 2, 3, 4]", list.toString());
        assertEquals(1, list.removeAt(0));
        assertEquals(4, list.removeAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
    }

    @Test
    void removeIfFilterFindAndCount() {
        for (int i = 1; i <= 6; i++) {
            list.add(i);
        }

        assertEquals(3, list.count(n -> n % 2 == 0));
        assertEquals("[2, 4, 6]", list.filter(n -> n % 2 == 0).toString());
        assertEquals(4, list.find(n -> n > 3));
        assertNull(list.find(n -> n > 10));

        assertTrue(list.removeIf(n -> n % 2 == 0));
        assertEquals("[1, 3, 5]", list.toString());
        assertEquals(5, list.getLast());
    }

    @Test
    void sortIsStableAndKeepsTailValid() {
        int[] values = {5, 3, 8, 1, 9, 2};
        for (int value : values) {
            list.add(value);
        }

        list.sort(Comparator.naturalOrder());
        list.add(10);

        assertEquals("[1, 2, 3, 5, 8, 9, 10]", list.toString());
    }
}
