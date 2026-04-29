package net.hollowcube.luau.slopgen.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserDataTagTableTest {

    @Test
    void allocatesSequentiallyStartingAtOne() {
        var t = new UserDataTagTable();
        assertEquals(1, t.allocate());
        assertEquals(2, t.allocate());
        assertEquals(3, t.allocate());
    }

    @Test
    void overflowAt255() {
        var t = new UserDataTagTable();
        for (int i = 1; i < 255; i++) t.allocate();
        assertThrows(IllegalStateException.class, t::allocate);
    }
}
