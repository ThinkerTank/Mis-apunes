package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * TAD Stack (LIFO) implemented on the project's own {@link LinkedList}.
 *
 * <p>The top of the stack is the head of the list, so push, pop and peek only touch the first
 * node and run in constant time. It stores the operator's movement history (undo with the U key)
 * and the log of administrative actions.
 *
 * @param <T> type of the stored elements
 */
public class Stack<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final LinkedList<T> items = new LinkedList<>();

    /**
     * Puts an element on top of the stack.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     */
    public void push(T item) {
        items.addFirst(item);
    }

    /**
     * Removes and returns the element on top of the stack.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @throws EmptyStructureException if the stack is empty
     */
    public T pop() {
        if (items.isEmpty()) {
            throw new EmptyStructureException("Cannot pop: the stack is empty");
        }
        return items.removeFirst();
    }

    /**
     * Returns the element on top of the stack without removing it.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @throws EmptyStructureException if the stack is empty
     */
    public T peek() {
        if (items.isEmpty()) {
            throw new EmptyStructureException("Cannot peek: the stack is empty");
        }
        return items.getFirst();
    }

    /** Whether the stack has no elements. Complexity — O(1). */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Number of elements in the stack. Complexity — O(1). */
    public int size() {
        return items.size();
    }

    /** Removes every element. Complexity — O(1). */
    public void clear() {
        items.clear();
    }

    /** Iterates from the top to the bottom of the stack. */
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public String toString() {
        return "Stack" + items;
    }
}
