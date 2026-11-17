package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * TAD Queue (FIFO) implemented on the project's own {@link LinkedList}, which keeps references to
 * the head (front of the queue) and the tail (back of the queue).
 *
 * <p>Enqueue appends at the tail and dequeue removes from the head, so both run in constant time.
 * It models the dispatch queue (incidents waiting for a vehicle, by order of arrival) and the
 * queue of simulation events consumed by the interface.
 *
 * @param <T> type of the stored elements
 */
public class Queue<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final LinkedList<T> items = new LinkedList<>();

    /**
     * Adds an element at the back of the queue.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     */
    public void enqueue(T item) {
        items.addLast(item);
    }

    /**
     * Removes and returns the element at the front of the queue.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @throws EmptyStructureException if the queue is empty
     */
    public T dequeue() {
        if (items.isEmpty()) {
            throw new EmptyStructureException("Cannot dequeue: the queue is empty");
        }
        return items.removeFirst();
    }

    /**
     * Returns the element at the front of the queue without removing it.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @throws EmptyStructureException if the queue is empty
     */
    public T front() {
        if (items.isEmpty()) {
            throw new EmptyStructureException("The queue is empty");
        }
        return items.getFirst();
    }

    /**
     * Removes a specific element wherever it is. This is not part of the classic TAD; the dispatch
     * queue needs it because an incident may receive a vehicle before reaching the front.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     *
     * @return {@code true} if the element was in the queue
     */
    public boolean remove(T item) {
        return items.remove(item);
    }

    /** Whether the element is waiting in the queue. Complexity — O(n). */
    public boolean contains(T item) {
        return items.contains(item);
    }

    /** Whether the queue has no elements. Complexity — O(1). */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Number of elements in the queue. Complexity — O(1). */
    public int size() {
        return items.size();
    }

    /** Removes every element. Complexity — O(1). */
    public void clear() {
        items.clear();
    }

    /** Iterates from the front to the back of the queue. */
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public String toString() {
        return "Queue" + items;
    }
}
