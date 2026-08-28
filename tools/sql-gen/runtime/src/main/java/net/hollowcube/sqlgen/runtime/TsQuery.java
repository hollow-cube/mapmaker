package net.hollowcube.sqlgen.runtime;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/// Turns what someone typed into a `tsquery`.
///
/// Every word has to match, and the last one is left open with `:*` so that results narrow as the
/// caller types rather than only landing on the last keystroke. Anything that is not a letter or a
/// digit is a separator, which is also what keeps the caller's text out of tsquery's own syntax —
/// `to_tsquery` parses its argument, so passing it through unfiltered is the full-text equivalent of
/// building SQL by concatenation.
public final class TsQuery {

    /// The tsquery for `text`, or null if there is no word in it to search for.
    public static @Nullable String of(String text) {
        var words = new ArrayList<String>();
        var word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            var c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else if (!word.isEmpty()) {
                words.add(word.toString());
                word.setLength(0);
            }
        }
        if (!word.isEmpty()) words.add(word.toString());
        if (words.isEmpty()) return null;

        words.set(words.size() - 1, words.getLast() + ":*");
        return String.join(" & ", words);
    }

    private TsQuery() {
    }
}
