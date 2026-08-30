package net.hollowcube.apiserver.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTextTest {

    @Test
    void strip_keepsAnythingTypedOnAKeyboard() {
        assertEquals("hello world!", ChatText.strip("hello world!"));
        assertEquals("~`!@#$%^&*()_+-=[]{}|;':\",./<>?", ChatText.strip("~`!@#$%^&*()_+-=[]{}|;':\",./<>?"));
    }

    @Test
    void strip_keepsLettersFromEveryScriptAndTheMarksOnThem() {
        assertEquals("café niño", ChatText.strip("café niño"));
        // The same word with the accent as a combining mark rather than a composed letter.
        assertEquals("café", ChatText.strip("café"));
        assertEquals("привет", ChatText.strip("привет"));
        assertEquals("こんにちは", ChatText.strip("こんにちは"));
    }

    @Test
    void strip_keepsCurrencySymbols() {
        // Not for their own sake: the filter folds them back into letters, so a message written with
        // one has to still have it to fold.
        assertEquals("€10 or £8 or ¥1000", ChatText.strip("€10 or £8 or ¥1000"));
    }

    @Test
    void strip_dropsEmoji() {
        assertEquals("hi ", ChatText.strip("hi 😀"));
        assertEquals("", ChatText.strip("🎉🎉"));
    }

    @Test
    void strip_dropsControlCharacters() {
        assertEquals("ab", ChatText.strip("a\tb"));
        assertEquals("ab", ChatText.strip("a\nb"));
        assertEquals("ab", ChatText.strip("a­b"));
    }

    @Test
    void strip_canLeaveNothing() {
        assertEquals("", ChatText.strip("😀​"));
    }
}
