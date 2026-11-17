package co.edu.icesi.sgmms.model.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnionFindTest {

    @Test
    void everyElementStartsInItsOwnSet() {
        UnionFind sets = new UnionFind(5);

        assertEquals(5, sets.componentCount());
        assertFalse(sets.connected(0, 1));
        assertEquals(3, sets.find(3));
    }

    @Test
    void unionMergesSetsAndIsTransitive() {
        UnionFind sets = new UnionFind(5);

        assertTrue(sets.union(0, 1));
        assertTrue(sets.union(1, 2));

        assertTrue(sets.connected(0, 2));
        assertEquals(3, sets.componentCount());
    }

    @Test
    void unionInsideTheSameSetReportsACycle() {
        UnionFind sets = new UnionFind(3);
        sets.union(0, 1);
        sets.union(1, 2);

        assertFalse(sets.union(0, 2));
        assertEquals(1, sets.componentCount());
    }

    @Test
    void longChainStillResolvesToOneRoot() {
        int n = 1000;
        UnionFind sets = new UnionFind(n);
        for (int i = 1; i < n; i++) {
            sets.union(i - 1, i);
        }

        int root = sets.find(0);
        for (int i = 0; i < n; i++) {
            assertEquals(root, sets.find(i));
        }
        assertEquals(1, sets.componentCount());
    }
}
