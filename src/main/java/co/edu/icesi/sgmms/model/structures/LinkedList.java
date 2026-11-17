package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Generic singly linked list with references to both ends.
 *
 * <p>It is the base of every linear structure in the project: stacks, queues, adjacency lists and
 * hash-table buckets are all built on top of it (technical requirement RT-02). Keeping a
 * {@code tail} reference makes insertion at the end O(1), which is what a FIFO queue needs.
 *
 * <p>Invariant: {@code size} equals the number of reachable nodes from {@code head};
 * {@code head == null} if and only if {@code tail == null} if and only if {@code size == 0}.
 *
 * @param <T> type of the stored elements
 */
public class LinkedList<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final class Node<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Inserts an element at the beginning of the list.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @param value element to insert
     */
    public void addFirst(T value) {
        Node<T> node = new Node<>(value);
        node.next = head;
        head = node;
        if (tail == null) {
            tail = node;
        }
        size++;
    }

    /**
     * Inserts an element at the end of the list.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1) thanks to the tail reference;
     * space: O(1).
     *
     * @param value element to insert
     */
    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    /** Same as {@link #addLast(Object)}. */
    public void add(T value) {
        addLast(value);
    }

    /**
     * Inserts an element so that it ends up at position {@code index}.
     *
     * <p>Complexity — time: best O(1) (index 0 or size), average O(n), worst O(n); space: O(1).
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, size]}
     */
    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index " + index + " outside [0, " + size + "]");
        }
        if (index == 0) {
            addFirst(value);
        } else if (index == size) {
            addLast(value);
        } else {
            Node<T> previous = nodeAt(index - 1);
            Node<T> node = new Node<>(value);
            node.next = previous.next;
            previous.next = node;
            size++;
        }
    }

    /**
     * Removes and returns the first element.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(1); space: O(1).
     *
     * @throws EmptyStructureException if the list is empty
     */
    public T removeFirst() {
        if (head == null) {
            throw new EmptyStructureException("La lista está vacía");
        }
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return value;
    }

    /**
     * Removes and returns the last element. A singly linked list must walk to the node before the
     * tail, so this is linear.
     *
     * <p>Complexity — time: best O(1) (one element), average O(n), worst O(n); space: O(1).
     *
     * @throws EmptyStructureException if the list is empty
     */
    public T removeLast() {
        if (head == null) {
            throw new EmptyStructureException("La lista está vacía");
        }
        if (head == tail) {
            return removeFirst();
        }
        Node<T> previous = head;
        while (previous.next != tail) {
            previous = previous.next;
        }
        T value = tail.value;
        previous.next = null;
        tail = previous;
        size--;
        return value;
    }

    /**
     * Returns the first element without removing it.
     *
     * <p>Complexity — time: O(1) in every case; space: O(1).
     *
     * @throws EmptyStructureException if the list is empty
     */
    public T getFirst() {
        if (head == null) {
            throw new EmptyStructureException("La lista está vacía");
        }
        return head.value;
    }

    /**
     * Returns the last element without removing it.
     *
     * <p>Complexity — time: O(1) in every case; space: O(1).
     *
     * @throws EmptyStructureException if the list is empty
     */
    public T getLast() {
        if (tail == null) {
            throw new EmptyStructureException("La lista está vacía");
        }
        return tail.value;
    }

    /**
     * Returns the element at {@code index}.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, size)}
     */
    public T get(int index) {
        return nodeAt(index).value;
    }

    /**
     * Replaces the element at {@code index}.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     */
    public void set(int index, T value) {
        nodeAt(index).value = value;
    }

    /**
     * Removes the element at {@code index} and returns it.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     */
    public T removeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " outside [0, " + size + ")");
        }
        if (index == 0) {
            return removeFirst();
        }
        Node<T> previous = nodeAt(index - 1);
        Node<T> removed = previous.next;
        previous.next = removed.next;
        if (removed == tail) {
            tail = previous;
        }
        size--;
        return removed.value;
    }

    /**
     * Removes the first element equal to {@code value}.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     *
     * @return {@code true} if an element was removed
     */
    public boolean remove(T value) {
        Node<T> previous = null;
        Node<T> current = head;
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                unlink(previous, current);
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /**
     * Removes every element that satisfies {@code condition}.
     *
     * <p>Complexity — time: O(n) in every case; space: O(1).
     *
     * @return {@code true} if at least one element was removed
     */
    public boolean removeIf(Predicate<? super T> condition) {
        boolean removedAny = false;
        Node<T> previous = null;
        Node<T> current = head;
        while (current != null) {
            Node<T> next = current.next;
            if (condition.test(current.value)) {
                unlink(previous, current);
                removedAny = true;
            } else {
                previous = current;
            }
            current = next;
        }
        return removedAny;
    }

    /**
     * Returns whether an element equal to {@code value} is present.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     */
    public boolean contains(T value) {
        return indexOf(value) >= 0;
    }

    /**
     * Returns the position of the first element equal to {@code value}, or -1.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     */
    public int indexOf(T value) {
        int index = 0;
        for (Node<T> current = head; current != null; current = current.next) {
            if (Objects.equals(current.value, value)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Returns the first element that satisfies {@code condition}, or {@code null}.
     *
     * <p>Complexity — time: best O(1), average O(n), worst O(n); space: O(1).
     */
    public T find(Predicate<? super T> condition) {
        for (Node<T> current = head; current != null; current = current.next) {
            if (condition.test(current.value)) {
                return current.value;
            }
        }
        return null;
    }

    /**
     * Returns a new list with the elements that satisfy {@code condition}, in the same order.
     *
     * <p>Complexity — time: O(n); space: O(k) for the k selected elements.
     */
    public LinkedList<T> filter(Predicate<? super T> condition) {
        LinkedList<T> result = new LinkedList<>();
        for (T value : this) {
            if (condition.test(value)) {
                result.addLast(value);
            }
        }
        return result;
    }

    /**
     * Counts the elements that satisfy {@code condition}.
     *
     * <p>Complexity — time: O(n); space: O(1).
     */
    public int count(Predicate<? super T> condition) {
        int count = 0;
        for (T value : this) {
            if (condition.test(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sorts the list in place with merge sort, which needs no random access and is stable.
     *
     * <p>Complexity — time: O(n log n) in the best, average and worst case; space: O(log n) for
     * the recursion stack (nodes are relinked, not copied).
     */
    public void sort(Comparator<? super T> comparator) {
        head = mergeSort(head, comparator);
        tail = head;
        if (tail != null) {
            while (tail.next != null) {
                tail = tail.next;
            }
        }
    }

    /** Returns a shallow copy of this list. Complexity — time: O(n); space: O(n). */
    public LinkedList<T> copy() {
        LinkedList<T> result = new LinkedList<>();
        for (T value : this) {
            result.addLast(value);
        }
        return result;
    }

    /** Number of elements. Complexity — O(1). */
    public int size() {
        return size;
    }

    /** Whether the list has no elements. Complexity — O(1). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every element. Complexity — O(1). */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for (Node<T> current = head; current != null; current = current.next) {
            builder.append(current.value);
            if (current.next != null) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    private Node<T> nodeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " outside [0, " + size + ")");
        }
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }

    private void unlink(Node<T> previous, Node<T> current) {
        if (previous == null) {
            head = current.next;
        } else {
            previous.next = current.next;
        }
        if (current == tail) {
            tail = previous;
        }
        size--;
    }

    private Node<T> mergeSort(Node<T> start, Comparator<? super T> comparator) {
        if (start == null || start.next == null) {
            return start;
        }
        Node<T> slow = start;
        Node<T> fast = start.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        Node<T> second = slow.next;
        slow.next = null;
        return merge(mergeSort(start, comparator), mergeSort(second, comparator), comparator);
    }

    private Node<T> merge(Node<T> left, Node<T> right, Comparator<? super T> comparator) {
        Node<T> dummy = new Node<>(null);
        Node<T> last = dummy;
        while (left != null && right != null) {
            if (comparator.compare(left.value, right.value) <= 0) {
                last.next = left;
                left = left.next;
            } else {
                last.next = right;
                right = right.next;
            }
            last = last.next;
        }
        last.next = (left != null) ? left : right;
        return dummy.next;
    }
}
