package co.edu.icesi.sgmms.model.structures;

import java.io.Serializable;

/**
 * Key-value pair stored inside a {@link HashTable} bucket.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class HashEntry<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final K key;
    private V value;

    public HashEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /** Returns the key. Complexity — time and space O(1). */
    public K getKey() {
        return key;
    }

    /** Returns the value. Complexity — time and space O(1). */
    public V getValue() {
        return value;
    }

    /** Replaces the value. Complexity — time and space O(1). */
    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
