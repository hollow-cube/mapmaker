package net.hollowcube.luau.slopgen.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtomTableTest {

    @Test
    void assignsSequentialValuesStartingAtOne() {
        var t = new AtomTable();
        assertEquals((short) 1, t.atomFor("a"));
        assertEquals((short) 2, t.atomFor("b"));
        assertEquals((short) 3, t.atomFor("c"));
    }

    @Test
    void deduplicatesByName() {
        var t = new AtomTable();
        short first = t.atomFor("foo");
        t.atomFor("bar");
        short second = t.atomFor("foo");
        assertEquals(first, second);
    }

    @Test
    void entriesReturnedSortedByValue() {
        var t = new AtomTable();
        t.atomFor("zebra");   // 1
        t.atomFor("apple");   // 2
        t.atomFor("mango");   // 3
        var entries = t.entries();
        assertEquals(List.of(
            new AtomTable.Entry("zebra", (short) 1),
            new AtomTable.Entry("apple", (short) 2),
            new AtomTable.Entry("mango", (short) 3)
        ), entries);
    }

    @Test
    void isEmptyReflectsContent() {
        var t = new AtomTable();
        assertTrue(t.isEmpty());
        t.atomFor("x");
        assertEquals(false, t.isEmpty());
    }

    @Test
    void overflowAtShortMaxValue() {
        var t = new AtomTable();
        for (int i = 1; i < Short.MAX_VALUE; i++)
            t.atomFor("a" + i);
        assertThrows(IllegalStateException.class, () -> t.atomFor("overflow"));
    }
}
