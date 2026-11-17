package co.edu.icesi.sgmms.model.structures;

import java.io.Serializable;

/**
 * TAD HashTable with separate chaining. Every bucket is one of the project's own
 * {@link LinkedList}s of {@link HashEntry} objects (technical requirement RT-02).
 *
 * <p><b>Hash function.</b> The bucket of a key is
 * {@code (h ^ (h >>> 16)) & (capacity - 1)}, where {@code h = key.hashCode()} and the capacity is
 * always a power of two. Masking with {@code capacity - 1} replaces the modulo with one cheap
 * bitwise operation, and XOR-ing the high half into the low half makes the upper bits of the
 * hash code take part in the index. Without that step, keys that differ only in their high bits
 * (for example, IDs such as "VEH-10", "VEH-20", ...) would pile up in the same buckets of a small
 * table.
 *
 * <p><b>Load factor.</b> When {@code size / capacity} would exceed {@value #MAX_LOAD_FACTOR}, the
 * capacity doubles and every entry is redistributed. This keeps the expected chain length
 * constant, which is what gives O(1) on average.
 *
 * @param <K> key type (null keys are not allowed)
 * @param <V> value type
 */
public class HashTable<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Maximum ratio of entries to buckets before the table grows. */
    public static final double MAX_LOAD_FACTOR = 0.75;

    private static final int DEFAULT_CAPACITY = 16;

    private LinkedList<HashEntry<K, V>>[] buckets;
    private int size;

    public HashTable() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a table whose capacity is the smallest power of two that is at least
     * {@code initialCapacity}.
     */
    public HashTable(int initialCapacity) {
        int capacity = 1;
        while (capacity < Math.max(initialCapacity, 1)) {
            capacity <<= 1;
        }
        buckets = newBuckets(capacity);
    }

    /**
     * Associates {@code value} with {@code key}, replacing a previous value if the key exists.
     *
     * <p>Complexity — time: best O(1), average O(1) (expected chain length bounded by the load
     * factor; resizing is amortized O(1)), worst O(n) when every key lands in the same bucket or a
     * resize happens; space: O(1) per entry, O(n) during a resize.
     *
     * @return the previous value of the key, or {@code null} if it was new
     * @throws IllegalArgumentException if {@code key} is null
     */
    public V put(K key, V value) {
        requireKey(key);
        LinkedList<HashEntry<K, V>> bucket = buckets[indexFor(key, buckets.length)];
        HashEntry<K, V> existing = findEntry(bucket, key);
        if (existing != null) {
            V previous = existing.getValue();
            existing.setValue(value);
            return previous;
        }
        if ((size + 1) > MAX_LOAD_FACTOR * buckets.length) {
            resize(buckets.length * 2);
            bucket = buckets[indexFor(key, buckets.length)];
        }
        bucket.addFirst(new HashEntry<>(key, value));
        size++;
        return null;
    }

    /**
     * Returns the value associated with {@code key}, or {@code null} if the key is absent.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(n) (all keys in one bucket);
     * space: O(1).
     */
    public V get(K key) {
        requireKey(key);
        HashEntry<K, V> entry = findEntry(buckets[indexFor(key, buckets.length)], key);
        return entry == null ? null : entry.getValue();
    }

    /**
     * Removes the entry of {@code key}.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(n); space: O(1).
     *
     * @return the removed value, or {@code null} if the key was absent
     */
    public V remove(K key) {
        requireKey(key);
        LinkedList<HashEntry<K, V>> bucket = buckets[indexFor(key, buckets.length)];
        HashEntry<K, V> entry = findEntry(bucket, key);
        if (entry == null) {
            return null;
        }
        bucket.remove(entry);
        size--;
        return entry.getValue();
    }

    /**
     * Whether {@code key} has an associated value.
     *
     * <p>Complexity — time: best O(1), average O(1), worst O(n); space: O(1).
     */
    public boolean containsKey(K key) {
        requireKey(key);
        return findEntry(buckets[indexFor(key, buckets.length)], key) != null;
    }

    /** Number of stored entries. Complexity — O(1). */
    public int size() {
        return size;
    }

    /** Whether the table has no entries. Complexity — O(1). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every entry and keeps the current capacity. Complexity — O(capacity). */
    public void clear() {
        buckets = newBuckets(buckets.length);
        size = 0;
    }

    /**
     * Returns all keys, in bucket order.
     *
     * <p>Complexity — time: O(n + capacity); space: O(n).
     */
    public LinkedList<K> keys() {
        LinkedList<K> keys = new LinkedList<>();
        for (LinkedList<HashEntry<K, V>> bucket : buckets) {
            for (HashEntry<K, V> entry : bucket) {
                keys.addLast(entry.getKey());
            }
        }
        return keys;
    }

    /**
     * Returns all values, in bucket order.
     *
     * <p>Complexity — time: O(n + capacity); space: O(n).
     */
    public LinkedList<V> values() {
        LinkedList<V> values = new LinkedList<>();
        for (LinkedList<HashEntry<K, V>> bucket : buckets) {
            for (HashEntry<K, V> entry : bucket) {
                values.addLast(entry.getValue());
            }
        }
        return values;
    }

    /** Current number of buckets. Complexity — O(1). */
    public int capacity() {
        return buckets.length;
    }

    /** Current ratio of entries to buckets. Complexity — O(1). */
    public double loadFactor() {
        return (double) size / buckets.length;
    }

    /**
     * Bucket index that {@code key} maps to with the current capacity. Exposed so the collision
     * behaviour can be analysed and tested. Complexity — O(1).
     */
    public int bucketIndexOf(K key) {
        requireKey(key);
        return indexFor(key, buckets.length);
    }

    /** Number of entries chained in bucket {@code index}. Complexity — O(1). */
    public int bucketSize(int index) {
        return buckets[index].size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (LinkedList<HashEntry<K, V>> bucket : buckets) {
            for (HashEntry<K, V> entry : bucket) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(entry);
                first = false;
            }
        }
        return builder.append('}').toString();
    }

    private static int indexFor(Object key, int capacity) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return h & (capacity - 1);
    }

    private HashEntry<K, V> findEntry(LinkedList<HashEntry<K, V>> bucket, K key) {
        return bucket.find(entry -> entry.getKey().equals(key));
    }

    private void resize(int newCapacity) {
        LinkedList<HashEntry<K, V>>[] old = buckets;
        buckets = newBuckets(newCapacity);
        for (LinkedList<HashEntry<K, V>> bucket : old) {
            for (HashEntry<K, V> entry : bucket) {
                buckets[indexFor(entry.getKey(), newCapacity)].addFirst(entry);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<HashEntry<K, V>>[] newBuckets(int capacity) {
        LinkedList<HashEntry<K, V>>[] array = (LinkedList<HashEntry<K, V>>[]) new LinkedList[capacity];
        for (int i = 0; i < capacity; i++) {
            array[i] = new LinkedList<>();
        }
        return array;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Las claves de la tabla hash no pueden ser nulas");
        }
    }
}
