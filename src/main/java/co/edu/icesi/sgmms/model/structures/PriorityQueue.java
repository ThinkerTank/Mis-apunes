package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * TAD PriorityQueue implemented as a binary max-heap over the project's own growable array.
 *
 * <p>The element with the greatest priority according to the comparator is always at index 0.
 * For a node at index {@code i}, its children are at {@code 2i + 1} and {@code 2i + 2} and its
 * parent at {@code (i - 1) / 2}. Heap invariant: no child compares greater than its parent.
 *
 * <p>A min-heap is obtained by passing a reversed comparator; Dijkstra and Prim use it that way.
 *
 * @param <T> type of the stored elements
 */
public class PriorityQueue<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_CAPACITY = 16;

    private final Comparator<? super T> comparator;
    private Object[] heap;
    private int size;

    /**
     * @param comparator defines priority: {@code compare(a, b) > 0} means {@code a} goes first.
     *                   It must be {@link Serializable} if the queue is going to be serialized.
     */
    public PriorityQueue(Comparator<? super T> comparator) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
        this.heap = new Object[DEFAULT_CAPACITY];
    }

    /**
     * Adds an element and restores the heap invariant by sifting it up.
     *
     * <p>Complexity — time: best O(1) (it stays at the bottom), average O(log n), worst O(log n)
     * (it climbs to the root; amortized O(1) extra for array growth); space: O(1) amortized.
     */
    public void insert(T element) {
        if (size == heap.length) {
            Object[] bigger = new Object[heap.length * 2];
            System.arraycopy(heap, 0, bigger, 0, heap.length);
            heap = bigger;
        }
        heap[size] = element;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the element with the greatest priority.
     *
     * <p>Complexity — time: best O(1), average O(log n), worst O(log n) (the last leaf sinks back
     * to the bottom); space: O(1).
     *
     * @throws EmptyStructureException if the queue is empty
     */
    public T extractMax() {
        T top = peekMax();
        size--;
        heap[0] = heap[size];
        heap[size] = null;
        if (size > 0) {
            siftDown(0);
        }
        return top;
    }

    /**
     * Returns the element with the greatest priority without removing it.
     *
     * <p>Complexity — time: O(1) in every case (it is always at index 0); space: O(1).
     *
     * @throws EmptyStructureException if the queue is empty
     */
    public T peekMax() {
        if (size == 0) {
            throw new EmptyStructureException("La cola de prioridad está vacía");
        }
        return elementAt(0);
    }

    /**
     * Removes an arbitrary element. Locating it requires a linear scan; the hole is then filled
     * with the last leaf, which is sifted up or down as needed.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     *
     * @return {@code true} if the element was present
     */
    public boolean remove(T element) {
        int index = indexOf(element);
        if (index < 0) {
            return false;
        }
        size--;
        if (index == size) {
            heap[size] = null;
            return true;
        }
        heap[index] = heap[size];
        heap[size] = null;
        siftDown(index);
        siftUp(index);
        return true;
    }

    /** Whether the element is in the queue. Complexity — O(n). */
    public boolean contains(T element) {
        return indexOf(element) >= 0;
    }

    /** Whether the queue has no elements. Complexity — O(1). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Number of elements. Complexity — O(1). */
    public int size() {
        return size;
    }

    /** Removes every element. Complexity — O(n). */
    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    /**
     * Returns the elements in heap (array) order, which is not fully sorted.
     *
     * <p>Complexity — time: O(n); space: O(n).
     */
    public LinkedList<T> toList() {
        LinkedList<T> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            list.addLast(elementAt(i));
        }
        return list;
    }

    private void siftUp(int index) {
        Object moving = heap[index];
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(moving, heap[parent]) <= 0) {
                break;
            }
            heap[index] = heap[parent];
            index = parent;
        }
        heap[index] = moving;
    }

    private void siftDown(int index) {
        Object moving = heap[index];
        int half = size / 2;
        while (index < half) {
            int child = 2 * index + 1;
            int right = child + 1;
            if (right < size && compare(heap[right], heap[child]) > 0) {
                child = right;
            }
            if (compare(moving, heap[child]) >= 0) {
                break;
            }
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = moving;
    }

    private int indexOf(T element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(heap[i], element)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private int compare(Object a, Object b) {
        return comparator.compare((T) a, (T) b);
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
        return (T) heap[index];
    }
}
