package net.hollowcube.apiserver.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTextTest {

    @Test
    void strip_keepsKeyboardCharactersLettersMarksAndCurrency() {
        assertEquals("hello world!", ChatText.strip("hello world!"));
        assertEquals(
            "~`!@#$%^&*()_+-=[]{}|;':\",./<>?",
            ChatText.strip("~`!@#$%^&*()_+-=[]{}|;':\",./<>?")
        );
        assertEquals("café niño", ChatText.strip("café niño"));
        // The same word with the accent as a combining mark rather than a composed letter.
        assertEquals("café", ChatText.strip("café"));
        assertEquals("привет", ChatText.strip("привет"));
        assertEquals("こんにちは", ChatText.strip("こんにちは"));
        // The filter folds currency symbols back into letters, so they have to survive to be folded.
        assertEquals("€10 or £8 or ¥1000", ChatText.strip("€10 or £8 or ¥1000"));
    }

    @Test
    void strip_dropsEmojiAndControlCharacters() {
        assertEquals("hi ", ChatText.strip("hi 😀"));
        assertEquals("", ChatText.strip("🎉🎉"));
        assertEquals("ab", ChatText.strip("a\tb"));
        assertEquals("ab", ChatText.strip("a\nb"));
        assertEquals("ab", ChatText.strip("a­b"));
    }
}
