package net.hollowcube.apiserver.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizedTest {

    @Test
    void foldsToLowercaseAlphanumerics() {
        assertEquals("helloworld", Sanitized.of("Hello, World.").text());
        assertEquals("helloworldi", Sanitized.of("Hello, World!").text()); // ! is an i
        assertEquals("shit", Sanitized.of("$h1t").text());
        assertEquals("cock", Sanitized.of("c()ck").text());
        assertEquals("o", Sanitized.of("()").text());
        assertEquals("c", Sanitized.of("(").text()); // an unpaired bracket is still a c
        assertEquals("fuck", Sanitized.of("fück").text());
        assertEquals("fuck", Sanitized.of("ｆｕｃｋ").text());
        assertEquals("fi", Sanitized.of("ﬁ").text()); // one code point, two letters
        assertEquals("", Sanitized.of("﷽").text()); // one code point, eighteen chars, none of them ours
        assertEquals("fifi", Sanitized.of("ﬁ﷽ﬁ").text()); // and the arrays grow past the input length
        assertEquals("rape", Sanitized.of("rаpe").text()); // cyrillic а
        assertEquals("", Sanitized.of("日本語 🎉").text());
        assertEquals("io", Sanitized.of("10").text());
        assertEquals("26", Sanitized.of("26").text());
    }

    @Test
    void tokenStartsAndTails_followTheOriginalWords() {
        var s = Sanitized.of("ab cde-f");
        assertEquals("abcdef", s.text());
        assertArrayEquals(new boolean[]{true, false, true, false, false, true}, s.tokenStart());
        assertArrayEquals(new int[]{1, 0, 2, 1, 0, 0}, s.tail());
    }

    @Test
    void everyDroppedCharIsASeparator() {
        assertArrayEquals(new boolean[]{true, true, true}, Sanitized.of("a😀b日c").tokenStart());
        // A stripped accent is not one: the letter it sat on carries the word.
        assertArrayEquals(new boolean[]{true, false, false}, Sanitized.of("fu\u0308c").tokenStart());
        // A folded pair is one char in one word.
        assertArrayEquals(new boolean[]{true, false, false}, Sanitized.of("c()c").tokenStart());
    }

    @Test
    void originsPointAtTheOriginalChars() {
        var s = Sanitized.of("😀 fü(k");
        assertEquals("fuck", s.text());
        assertArrayEquals(new int[]{3, 4, 5, 6}, s.originStart());
        assertArrayEquals(new int[]{4, 5, 6, 7}, s.originEnd());

        s = Sanitized.of("c()ck");
        assertArrayEquals(new int[]{0, 1, 3, 4}, s.originStart());
        assertArrayEquals(new int[]{1, 3, 4, 5}, s.originEnd());

        s = Sanitized.of("u\u0308"); // combining mark is its own char in the original
        assertEquals("u", s.text());
        assertArrayEquals(new int[]{0}, s.originStart());
        assertArrayEquals(new int[]{1}, s.originEnd());

        s = Sanitized.of("ﬁ"); // one code point folding to two chars: both own it
        assertArrayEquals(new int[]{0, 0}, s.originStart());
        assertArrayEquals(new int[]{1, 1}, s.originEnd());
    }

    @Test
    void crosses() {
        var s = Sanitized.of("ab cd");
        assertFalse(s.crosses(0, 2));
        assertTrue(s.crosses(0, 3));
        assertTrue(s.crosses(1, 3));
        assertFalse(s.crosses(2, 4));
        assertFalse(s.crosses(3, 4));
    }
}
