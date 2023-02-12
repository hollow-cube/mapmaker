package net.hollowcube.canvas.internal.standalone.sprite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFontUtil {

    @Test
    public void testEmpty() {
        var text = FontUtil.computeOffset(0);
        assertEquals("", text);
    }

    @Test
    public void testSmallCases() {
        var text1 = FontUtil.computeOffset(1);
        assertEquals("0", text1);

        var text2 = FontUtil.computeOffset(25);
        assertEquals("034", text2);
    }

}
