package net.hollowcube.apiserver.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrieTest {

    private static Trie trie() {
        var trie = new Trie();
        trie.put("shit", List.of("horseshit", "bullshit"));
        trie.put("fuck", List.of());
        trie.put("ab", List.of("cabd"));
        trie.put("abcd", List.of());
        return trie;
    }

    private static int at(Trie trie, String text, int from) {
        return trie.longestAt(Sanitized.of(text), from);
    }

    @Test
    void matchesAtAnIndex() {
        var trie = trie();
        // Indices are into the folded text, `thisshit`.
        assertEquals(8, at(trie, "this shit", 4));
        assertEquals(-1, at(trie, "this shit", 3));
        assertEquals(-1, at(trie, "this shit", 0));
        assertEquals(-1, at(trie, "this shi", 4));
        assertEquals(-1, at(trie, "", 0));
    }

    @Test
    void negativesDropTheMatch() {
        var trie = trie();
        assertEquals(-1, at(trie, "horseshit", 5));
        assertEquals(-1, at(trie, "bullshit", 4));
        assertEquals(9, at(trie, "horse shit", 5)); // a negative is one word
        assertEquals(4, at(trie, "fuck bullshit", 0));
        assertEquals(-1, at(trie, "fuck bullshit", 8));
    }

    @Test
    void longestTermWins_andANegatedShortOneNeitherHidesNorFakesIt() {
        var trie = trie();
        assertEquals(4, at(trie, "abcd", 0));
        assertEquals(2, at(trie, "abxx", 0));
        assertEquals(-1, at(trie, "cabd", 1)); // ab negated by cabd, abcd does not continue
        assertEquals(5, at(trie, "cabcd", 1)); // ab is not negated here, and abcd is longer
        var negatedPrefix = new Trie();
        negatedPrefix.put("ab", List.of("abcd")); // the short term is negated exactly where the long one matches
        negatedPrefix.put("abcd", List.of());
        assertEquals(4, at(negatedPrefix, "abcd", 0));
        assertEquals(2, at(negatedPrefix, "abxy", 0));
    }

    @Test
    void crossingAWordBoundary_needsTheSpelledOutShape() {
        var trie = trie();
        assertEquals(-1, at(trie, "mass hit", 3));
        assertEquals(-1, at(trie, "this hitbox", 3));
        assertEquals(4, at(trie, "sh it", 0));
        assertEquals(4, at(trie, "s h i t", 0));
        assertEquals(4, at(trie, "shi ts", 0)); // one char of the next word left over
        assertEquals(-1, at(trie, "shi tstorm", 0)); // more than one
    }

    @Test
    void rejectsBadTerms() {
        var trie = new Trie();
        assertThrows(IllegalArgumentException.class, () -> trie.put("", List.of()));
        assertThrows(IllegalArgumentException.class, () -> trie.put("Bad", List.of()));
        assertThrows(IllegalArgumentException.class, () -> trie.put("bad", List.of("good")));
        assertThrows(IllegalArgumentException.class, () -> trie.put("bad", List.of("bad word")));
        trie.put("bad", List.of());
        assertThrows(IllegalArgumentException.class, () -> trie.put("bad", List.of()));
    }
}
