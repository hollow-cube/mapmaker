package net.hollowcube.command.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStringReader {

    @Test
    void testWordAlphanumeric() {
        var reader = new StringReader("abc123");
        var word = reader.readWord(WordType.ALPHANUMERIC);
        assertEquals("abc123", word);
    }

    @Test
    void testMultiWord() {
        var reader = new StringReader("the quick");
        assertEquals("the", reader.readWord(WordType.ALPHANUMERIC));
        assertEquals("quick", reader.readWord(WordType.ALPHANUMERIC));
        assertEquals("", reader.readWord(WordType.ALPHANUMERIC));
    }
}
