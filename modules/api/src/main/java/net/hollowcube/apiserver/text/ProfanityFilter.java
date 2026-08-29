package net.hollowcube.apiserver.text;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/// Finds terms from a list in free text, through the usual disguises — case, accents, leetspeak,
/// lookalike letters, spacing the word out — without tripping on the words that merely contain
/// one. Runs in-process in microseconds; nothing here calls out.
///
/// The text is folded to `[a-z0-9]` by [Sanitized] and scanned with a [Trie] of the terms. See
/// [Trie#longestAt] for how a term spread over several words is told from two innocent words that
/// happen to spell one across their seam, and [Profanities] for the list, which is the only one.
public final class ProfanityFilter {

    public record Match(String term, int start, int end) {}

    public record Result(String text, List<Match> matches, BitSet mask) {

        public boolean matched() {
            return !matches.isEmpty();
        }

        public String censored(char with) {
            var out = new StringBuilder(text);
            for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1))
                out.setCharAt(i, with);
            return out.toString();
        }
    }

    private ProfanityFilter() {}

    public static Result test(String text) {
        var s = Sanitized.of(text);
        var folded = s.text();
        var matches = new ArrayList<Match>();
        var mask = new BitSet(text.length());
        for (int i = 0; i < folded.length(); ) {
            int end = Profanities.TERM_TRIE.longestAt(s, i);
            if (end < 0) {
                i++;
                continue;
            }
            int start = s.originStart()[i], stop = s.originEnd()[end - 1];
            matches.add(new Match(folded.substring(i, end), start, stop));
            mask.set(start, stop);
            i = end;
        }
        return new Result(text, List.copyOf(matches), mask);
    }
}
