package co.edu.icesi.sgmms.model.structures;

import co.edu.icesi.sgmms.model.exceptions.EmptyStructureException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Generic binary search tree ordered by a comparator.
 *
 * <p>Invariant: every element in the left subtree of a node compares lower than the node and
 * every element in the right subtree compares greater. Elements that compare equal are treated as
 * duplicates and rejected, so the comparator must break every tie (the incident comparator ends
 * with the incident ID for that reason).
 *
 * <p>For incidents, "greater" means "more urgent", so the maximum is the incident with the highest
 * priority and a reverse in-order traversal lists them from most to least urgent.
 *
 * @param <T> type of the stored elements
 */
public class BinarySearchTree<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final class TreeNode<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private T value;
        private TreeNode<T> left;
        private TreeNode<T> right;

        private TreeNode(T value) {
            this.value = value;
        }
    }

    private final Comparator<? super T> comparator;
    private TreeNode<T> root;
    private int size;

    /** @param comparator total order of the elements; must be {@link Serializable} to persist. */
    public BinarySearchTree(Comparator<? super T> comparator) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    /**
     * Inserts an element in its ordered position.
     *
     * <p>Complexity — time: best O(1) (empty tree), average O(log n) (random insertion order),
     * worst O(n) (degenerate tree from sorted insertions); space: O(h) for the recursion, where
     * h is the height.
     *
     * @return {@code false} if an equal element was already present
     */
    public boolean insert(T value) {
        int before = size;
        root = insert(root, value);
        return size > before;
    }

    /**
     * Removes an element. A node with two children is replaced by its in-order successor.
     *
     * <p>Complexity — time: best O(1), average O(log n), worst O(n); space: O(h).
     *
     * @return {@code true} if the element was present
     */
    public boolean remove(T value) {
        int before = size;
        root = remove(root, value);
        return size < before;
    }

    /**
     * Whether an element equal to {@code value} is in the tree.
     *
     * <p>Complexity — time: best O(1), average O(log n), worst O(n); space: O(1).
     */
    public boolean contains(T value) {
        TreeNode<T> current = root;
        while (current != null) {
            int cmp = comparator.compare(value, current.value);
            if (cmp == 0) {
                return true;
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return false;
    }

    /**
     * Returns the greatest element (the rightmost node).
     *
     * <p>Complexity — time: best O(1), average O(log n), worst O(n); space: O(1).
     *
     * @throws EmptyStructureException if the tree is empty
     */
    public T max() {
        if (root == null) {
            throw new EmptyStructureException("El árbol está vacío");
        }
        TreeNode<T> current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.value;
    }

    /**
     * Returns the smallest element (the leftmost node).
     *
     * <p>Complexity — time: best O(1), average O(log n), worst O(n); space: O(1).
     *
     * @throws EmptyStructureException if the tree is empty
     */
    public T min() {
        if (root == null) {
            throw new EmptyStructureException("El árbol está vacío");
        }
        TreeNode<T> current = root;
        while (current.left != null) {
            current = current.left;
        }
        return current.value;
    }

    /**
     * Returns every element in ascending order (in-order traversal).
     *
     * <p>Complexity — time: O(n) in every case; space: O(n) for the result plus O(h) recursion.
     */
    public LinkedList<T> inOrder() {
        LinkedList<T> result = new LinkedList<>();
        inOrder(root, result);
        return result;
    }

    /**
     * Returns every element in descending order (reverse in-order traversal). For incidents this
     * is the list from the most to the least urgent.
     *
     * <p>Complexity — time: O(n) in every case; space: O(n) plus O(h) recursion.
     */
    public LinkedList<T> reverseInOrder() {
        LinkedList<T> result = new LinkedList<>();
        reverseInOrder(root, result);
        return result;
    }

    /**
     * Returns the elements that satisfy {@code condition}, in descending order. Used for queries
     * such as "every high-severity incident".
     *
     * <p>Complexity — time: O(n); space: O(k + h) for k matches.
     */
    public LinkedList<T> findAll(Predicate<? super T> condition) {
        LinkedList<T> result = new LinkedList<>();
        for (T value : reverseInOrder()) {
            if (condition.test(value)) {
                result.addLast(value);
            }
        }
        return result;
    }

    /** Height of the tree (an empty tree has height 0). Complexity — time O(n), space O(h). */
    public int height() {
        return height(root);
    }

    /** Number of elements. Complexity — O(1). */
    public int size() {
        return size;
    }

    /** Whether the tree has no elements. Complexity — O(1). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every element. Complexity — O(1). */
    public void clear() {
        root = null;
        size = 0;
    }

    private TreeNode<T> insert(TreeNode<T> node, T value) {
        if (node == null) {
            size++;
            return new TreeNode<>(value);
        }
        int cmp = comparator.compare(value, node.value);
        if (cmp < 0) {
            node.left = insert(node.left, value);
        } else if (cmp > 0) {
            node.right = insert(node.right, value);
        }
        return node;
    }

    private TreeNode<T> remove(TreeNode<T> node, T value) {
        if (node == null) {
            return null;
        }
        int cmp = comparator.compare(value, node.value);
        if (cmp < 0) {
            node.left = remove(node.left, value);
        } else if (cmp > 0) {
            node.right = remove(node.right, value);
        } else {
            if (node.left == null || node.right == null) {
                size--;
                return node.left != null ? node.left : node.right;
            }
            TreeNode<T> successor = node.right;
            while (successor.left != null) {
                successor = successor.left;
            }
            node.value = successor.value;
            node.right = remove(node.right, successor.value);
        }
        return node;
    }

    private void inOrder(TreeNode<T> node, LinkedList<T> result) {
        if (node != null) {
            inOrder(node.left, result);
            result.addLast(node.value);
            inOrder(node.right, result);
        }
    }

    private void reverseInOrder(TreeNode<T> node, LinkedList<T> result) {
        if (node != null) {
            reverseInOrder(node.right, result);
            result.addLast(node.value);
            reverseInOrder(node.left, result);
        }
    }

    private int height(TreeNode<T> node) {
        return node == null ? 0 : 1 + Math.max(height(node.left), height(node.right));
    }
}
