package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinarySearchTreeTest {

    private BinarySearchTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new BinarySearchTree<>(Comparator.naturalOrder());
        int[] values = {50, 30, 70, 20, 40, 60, 80};
        for (int value : values) {
            tree.insert(value);
        }
    }

    @Test
    void inOrderIsAscendingAndReverseIsDescending() {
        assertEquals("[20, 30, 40, 50, 60, 70, 80]", tree.inOrder().toString());
        assertEquals("[80, 70, 60, 50, 40, 30, 20]", tree.reverseInOrder().toString());
    }

    @Test
    void maxAndMinAreTheExtremes() {
        assertEquals(80, tree.max());
        assertEquals(20, tree.min());
    }

    @Test
    void duplicatesAreRejected() {
        assertFalse(tree.insert(40));
        assertEquals(7, tree.size());
    }

    @Test
    void removeLeafNodeWithOneChildAndNodeWithTwoChildren() {
        tree.insert(65);

        assertTrue(tree.remove(20));
        assertTrue(tree.remove(60));
        assertTrue(tree.remove(50));
        assertFalse(tree.remove(999));

        assertEquals("[30, 40, 65, 70, 80]", tree.inOrder().toString());
        assertEquals(5, tree.size());
        assertFalse(tree.contains(50));
        assertTrue(tree.contains(65));
    }

    @Test
    void emptyTreeThrowsOnMaxAndMin() {
        BinarySearchTree<Integer> empty = new BinarySearchTree<>(Comparator.naturalOrder());
        assertThrows(EmptyStructureException.class, empty::max);
        assertThrows(EmptyStructureException.class, empty::min);
        assertEquals(0, empty.height());
    }

    @Test
    void sortedInsertionsDegenerateIntoALinearHeight() {
        BinarySearchTree<Integer> degenerate = new BinarySearchTree<>(Comparator.naturalOrder());
        for (int i = 1; i <= 10; i++) {
            degenerate.insert(i);
        }

        assertEquals(10, degenerate.height());
        assertEquals(3, tree.height());
    }

    @Test
    void findAllReturnsMatchesInDescendingOrder() {
        assertEquals("[80, 60, 40, 20]", tree.findAll(n -> n % 20 == 0).toString());
    }
}
