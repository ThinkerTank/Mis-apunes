package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HashTableTest {

    /** Key whose hash code is fixed, so two different keys are forced into the same bucket. */
    private static final class CollidingKey {
        private final String name;

        CollidingKey(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CollidingKey && ((CollidingKey) other).name.equals(name);
        }
    }

    private HashTable<String, Integer> table;

    @BeforeEach
    void setUp() {
        table = new HashTable<>();
    }

    @Test
    void putAndGetReturnStoredValues() {
        table.put("VEH-1", 1);
        table.put("VEH-2", 2);

        assertEquals(1, table.get("VEH-1"));
        assertEquals(2, table.get("VEH-2"));
        assertNull(table.get("VEH-404"));
        assertEquals(2, table.size());
    }

    @Test
    void putOnExistingKeyReplacesValueWithoutGrowing() {
        table.put("VEH-1", 1);

        assertEquals(1, table.put("VEH-1", 10));
        assertEquals(10, table.get("VEH-1"));
        assertEquals(1, table.size());
    }

    @Test
    void removeAndContainsKey() {
        table.put("INC-1", 1);
        table.put("INC-2", 2);

        assertEquals(1, table.remove("INC-1"));
        assertFalse(table.containsKey("INC-1"));
        assertTrue(table.containsKey("INC-2"));
        assertNull(table.remove("INC-1"));
        assertEquals(1, table.size());
    }

    @Test
    void twoKeysInTheSameBucketAreBothRetrievable() {
        HashTable<CollidingKey, String> colliding = new HashTable<>();
        CollidingKey patrol = new CollidingKey("patrol");
        CollidingKey ambulance = new CollidingKey("ambulance");

        colliding.put(patrol, "P-1");
        colliding.put(ambulance, "A-1");

        int bucket = colliding.bucketIndexOf(patrol);
        assertEquals(bucket, colliding.bucketIndexOf(ambulance));
        assertEquals(2, colliding.bucketSize(bucket));
        assertEquals("P-1", colliding.get(patrol));
        assertEquals("A-1", colliding.get(ambulance));

        colliding.remove(patrol);
        assertEquals(1, colliding.bucketSize(bucket));
        assertEquals("A-1", colliding.get(ambulance));
    }

    @Test
    void tableGrowsAndKeepsLoadFactorUnderLimit() {
        int initialCapacity = table.capacity();
        for (int i = 0; i < 100; i++) {
            table.put("KEY-" + i, i);
        }

        assertTrue(table.capacity() > initialCapacity);
        assertTrue(table.loadFactor() <= HashTable.MAX_LOAD_FACTOR);
        for (int i = 0; i < 100; i++) {
            assertEquals(i, table.get("KEY-" + i));
        }
        assertEquals(100, table.keys().size());
        assertEquals(100, table.values().size());
    }

    @Test
    void nullKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> table.put(null, 1));
        assertThrows(IllegalArgumentException.class, () -> table.get(null));
    }
}
