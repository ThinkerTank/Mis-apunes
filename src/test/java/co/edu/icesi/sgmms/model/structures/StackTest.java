package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackTest {

    private Stack<String> stack;

    @BeforeEach
    void setUp() {
        stack = new Stack<>();
    }

    @Test
    void newStackIsEmpty() {
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    void popReturnsElementsInReverseOrder() {
        stack.push("W");
        stack.push("A");
        stack.push("S");

        assertEquals("S", stack.pop());
        assertEquals("A", stack.pop());
        assertEquals("W", stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void peekDoesNotRemove() {
        stack.push("assign");
        stack.push("resolve");

        assertEquals("resolve", stack.peek());
        assertEquals(2, stack.size());
    }

    @Test
    void popAndPeekOnEmptyStackThrow() {
        assertThrows(EmptyStructureException.class, stack::pop);
        assertThrows(EmptyStructureException.class, stack::peek);
    }

    @Test
    void stackIsReusableAfterBeingEmptied() {
        stack.push("one");
        stack.pop();
        stack.push("two");

        assertFalse(stack.isEmpty());
        assertEquals("two", stack.peek());
    }
}
