package net.hollowcube.luau.slopgen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentsTest {

    @Test
    void atomsAssignSequentialValuesStartingAtOne() {
        var ids = new Idents();
        assertEquals((short) 1, ids.atomFor("a"));
        assertEquals((short) 2, ids.atomFor("b"));
        assertEquals((short) 3, ids.atomFor("c"));
    }

    @Test
    void atomsDeduplicateByName() {
        var ids = new Idents();
        short first = ids.atomFor("foo");
        ids.atomFor("bar");
        short second = ids.atomFor("foo");
        assertEquals(first, second);
    }

    @Test
    void atomEntriesReturnedSortedByValue() {
        var ids = new Idents();
        ids.atomFor("zebra");   // 1
        ids.atomFor("apple");   // 2
        ids.atomFor("mango");   // 3
        var entries = ids.entries();
        assertEquals(List.of(
            new Idents.StringAtom("zebra", (short) 1),
            new Idents.StringAtom("apple", (short) 2),
            new Idents.StringAtom("mango", (short) 3)
        ), entries);
    }

    @Test
    void isEmptyReflectsAtomState() {
        var ids = new Idents();
        assertTrue(ids.isEmpty());
        ids.atomFor("x");
        assertFalse(ids.isEmpty());
    }

    @Test
    void atomOverflowAtShortMaxValue() {
        var ids = new Idents();
        for (int i = 1; i < Short.MAX_VALUE; i++)
            ids.atomFor("a" + i);
        assertThrows(IllegalStateException.class, () -> ids.atomFor("overflow"));
    }

    @Test
    void userDataTagsAllocateSequentiallyStartingAtOne() {
        var ids = new Idents();
        assertEquals(1, ids.allocUserDataTag());
        assertEquals(2, ids.allocUserDataTag());
        assertEquals(3, ids.allocUserDataTag());
    }

    @Test
    void userDataTagOverflowAt255() {
        var ids = new Idents();
        for (int i = 1; i < 255; i++) ids.allocUserDataTag();
        assertThrows(IllegalStateException.class, ids::allocUserDataTag);
    }

    @Test
    void lightUserDataTagsAllocateSequentiallyStartingAtOne() {
        var ids = new Idents();
        assertEquals(1, ids.allocLightUserDataTag());
        assertEquals(2, ids.allocLightUserDataTag());
    }

    @Test
    void lightUserDataTagOverflowAt255() {
        var ids = new Idents();
        for (int i = 1; i < 255; i++) ids.allocLightUserDataTag();
        assertThrows(IllegalStateException.class, ids::allocLightUserDataTag);
    }
}
