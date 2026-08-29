package net.hollowcube.apiserver.text;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/// `raw` folded to the `[a-z0-9]` the term list is written in — `$h1t` is `shit`, `fück` is
/// `fuck`, `ｆuck` is `fuck` — with what the folding throws away kept alongside, since the filter
/// needs both: where each folded char came from, so a match can be reported and masked against
/// the original, and where the words were, so a match may tell `r a p e` from `elyt(ra pe)arl`.
///
/// Every original char that does not survive is a separator. `tokenStart[i]` is set when folded
/// char `i` follows a separator (or is first), `tail[i]` is how many folded chars follow `i` in
/// the same word, and `[originStart[i], originEnd[i])` is the range of `raw` it came from.
record Sanitized(String raw, String text, boolean[] tokenStart, int[] tail, int[] originStart, int[] originEnd) {

    /// Substitutions the leetspeak-inclined make, folded back before matching.
    private static final Map<Character, Character> REPLACEMENTS = Map.ofEntries(
        Map.entry('4', 'a'), Map.entry('@', 'a'),
        Map.entry('3', 'e'), Map.entry('£', 'e'), Map.entry('€', 'e'),
        Map.entry('1', 'i'), Map.entry('!', 'i'), Map.entry('|', 'i'),
        Map.entry('0', 'o'),
        Map.entry('5', 's'), Map.entry('$', 's'),
        Map.entry('7', 't'), Map.entry('+', 't'),
        Map.entry('8', 'b'),
        Map.entry('9', 'g'),
        Map.entry('¥', 'y'),
        Map.entry('¢', 'c'), Map.entry('(', 'c'), Map.entry('{', 'c'), Map.entry('[', 'c'), Map.entry('<', 'c'),
        // Letters from other scripts that render the same as a latin one. Normalization does not
        // touch them, and dropping them would leave `rаpe` (cyrillic а) reading as `rpe`.
        Map.entry('а', 'a'), Map.entry('е', 'e'), Map.entry('о', 'o'), Map.entry('р', 'p'), Map.entry('с', 'c'),
        Map.entry('х', 'x'), Map.entry('у', 'y'), Map.entry('і', 'i'), Map.entry('ѕ', 's'), Map.entry('ј', 'j'),
        Map.entry('һ', 'h'), Map.entry('к', 'k'), Map.entry('м', 'm'), Map.entry('т', 't'), Map.entry('в', 'b'),
        Map.entry('н', 'h'), Map.entry('ԁ', 'd'), Map.entry('ԝ', 'w'), Map.entry('ԛ', 'q'),
        Map.entry('α', 'a'), Map.entry('ε', 'e'), Map.entry('ι', 'i'), Map.entry('κ', 'k'), Map.entry('ν', 'v'),
        Map.entry('ο', 'o'), Map.entry('ρ', 'p'), Map.entry('τ', 't'), Map.entry('υ', 'u'), Map.entry('χ', 'x'),
        Map.entry('β', 'b'), Map.entry('η', 'n'),
        Map.entry('ı', 'i'), Map.entry('ł', 'l'), Map.entry('ø', 'o'), Map.entry('đ', 'd'), Map.entry('ħ', 'h'),
        Map.entry('ŧ', 't')
    );

    /// Pairs that draw a letter between them — `c()ck`.
    private static final Map<Character, Map<Character, Character>> PAIR_REPLACEMENTS = Map.of(
        '(', Map.of(')', 'o'),
        '[', Map.of(']', 'o'),
        '{', Map.of('}', 'o'),
        '<', Map.of('>', 'o')
    );

    static Sanitized of(String raw) {
        var out = new Output(raw.length());
        var inToken = false;

        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int width = Character.charCount(cp);

            var pair = cp < 0x80 ? PAIR_REPLACEMENTS.get((char) cp) : null;
            if (pair != null && i + 1 < raw.length()) {
                var drawn = pair.get(raw.charAt(i + 1));
                if (drawn != null) {
                    out.add(drawn, !inToken, i, i + 2);
                    inToken = true;
                    i += 2;
                    continue;
                }
            }

            var survived = false;
            var onlyMarks = true;
            for (var c : fold(cp).toCharArray()) {
                if (Character.getType(c) == Character.NON_SPACING_MARK) continue; // the accent off an é
                onlyMarks = false;
                var replaced = REPLACEMENTS.get(c);
                if (replaced != null) c = replaced;
                else if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')) continue;
                out.add(c, !inToken, i, i + width);
                inToken = true;
                survived = true;
            }
            // A bare combining mark sits on the letter before it; it is not a gap in the word.
            if (!survived && !onlyMarks) inToken = false;
            i += width;
        }
        return out.finish(raw);
    }

    /// The folded chars as they are produced. Usually no longer than the input, but a single
    /// code point can fold to several chars (`ﬁ`, or `﷽` to eighteen), so it grows.
    private static final class Output {
        private final StringBuilder text;
        private boolean[] tokenStart;
        private int[] originStart, originEnd;

        Output(int expected) {
            text = new StringBuilder(expected);
            tokenStart = new boolean[expected];
            originStart = new int[expected];
            originEnd = new int[expected];
        }

        void add(char c, boolean startsToken, int from, int to) {
            int n = text.length();
            if (n == tokenStart.length) {
                int grown = Math.max(8, n * 2);
                tokenStart = Arrays.copyOf(tokenStart, grown);
                originStart = Arrays.copyOf(originStart, grown);
                originEnd = Arrays.copyOf(originEnd, grown);
            }
            tokenStart[n] = startsToken;
            originStart[n] = from;
            originEnd[n] = to;
            text.append(c);
        }

        Sanitized finish(String raw) {
            int n = text.length();
            var tail = new int[n];
            for (int i = n - 1, run = 0; i >= 0; i--) {
                tail[i] = run;
                run = tokenStart[i] ? 0 : run + 1;
            }
            return new Sanitized(raw, text.toString(), Arrays.copyOf(tokenStart, n), tail,
                Arrays.copyOf(originStart, n), Arrays.copyOf(originEnd, n));
        }
    }

    /// One code point, decomposed (NFKD) so a precomposed `é` splits into `e` plus its accent and
    /// compatibility forms like fullwidth `ｆ` or the `ﬁ` ligature come out as plain letters, then
    /// lowercased. Per code point rather than over the whole string so the origin of each output
    /// char is known; nothing the filter cares about composes across code points.
    private static String fold(int cp) {
        var s = Character.toString(cp);
        var decomposed = Normalizer.isNormalized(s, Normalizer.Form.NFKD) ? s : Normalizer.normalize(s, Normalizer.Form.NFKD);
        return decomposed.toLowerCase(Locale.ROOT);
    }

    /// Whether a match over folded chars `[from, to)` runs across a word boundary.
    boolean crosses(int from, int to) {
        for (int i = from + 1; i < to; i++)
            if (tokenStart[i]) return true;
        return false;
    }
}
