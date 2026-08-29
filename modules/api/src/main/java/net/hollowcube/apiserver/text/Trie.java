package net.hollowcube.apiserver.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The term list as a trie over `[a-z0-9]`, matched against a [Sanitized] text.
///
/// A term may carry negatives: longer words it is a substring of that are fine — `cockpit`,
/// `scunthorpe`, `document`. Each is kept as the prefix and suffix around the term and a match
/// is dropped when the folded text has them around it.
final class Trie {

    private static final int ALPHABET = 26 + 10;

    private record Negative(String prefix, String suffix) {
        /// A negative is one word, so besides being around the match it must not run across a
        /// word seam: `bull shit` is not `bullshit`.
        boolean surrounds(Sanitized s, int start, int end) {
            var text = s.text();
            int from = start - prefix.length(), to = end + suffix.length();
            return from >= 0 && to <= text.length()
                && text.startsWith(prefix, from) && text.startsWith(suffix, end)
                && !s.crosses(from, to);
        }
    }

    private static final class Node {
        final Node[] children = new Node[ALPHABET];
        boolean end;
        List<Negative> negatives = List.of();
    }

    private final Node root = new Node();
    private final Map<String, List<String>> terms = new LinkedHashMap<>();

    void put(String term, List<String> negatives) {
        if (term.isEmpty()) throw new IllegalArgumentException("empty term");
        for (var c : term.toCharArray())
            if (slot(c) < 0) throw new IllegalArgumentException("term '" + term + "' is not [a-z0-9]");
        var parsed = new ArrayList<Negative>(negatives.size());
        for (var negative : negatives) {
            int at = negative.indexOf(term);
            if (at < 0) throw new IllegalArgumentException("'" + negative + "' does not contain its term '" + term + "'");
            for (var c : negative.toCharArray())
                if (slot(c) < 0) throw new IllegalArgumentException("negative '" + negative + "' is not [a-z0-9]");
            parsed.add(new Negative(negative.substring(0, at), negative.substring(at + term.length())));
        }

        var node = root;
        for (var c : term.toCharArray()) {
            int slot = slot(c);
            if (node.children[slot] == null) node.children[slot] = new Node();
            node = node.children[slot];
        }
        if (node.end) throw new IllegalArgumentException("term '" + term + "' listed twice");
        node.end = true;
        node.negatives = parsed;
        terms.put(term, List.copyOf(negatives));
    }

    /// Every term with its negatives as put, for a test to walk.
    Map<String, List<String>> terms() {
        return terms;
    }

    /// The end (exclusive) of the longest term starting at folded index `from`, or -1.
    ///
    /// The folded text has no spaces, so a term can be found running across two words —
    /// `elytra pearl` holds `rape`. That is allowed only when the writer plainly spelled the term
    /// out in pieces: the match starts where a word does and ends no more than one char short of
    /// where one does, so `r a p e`, `sh it` and `ass hole` (stem `asshol`) are caught while
    /// `elytra pearl`, `mass hit` and `who responded` are not.
    int longestAt(Sanitized s, int from) {
        var text = s.text();
        var node = root;
        int best = -1;
        for (int i = from; i < text.length(); i++) {
            node = node.children[slot(text.charAt(i))];
            if (node == null) break;
            if (!node.end) continue;
            if (s.crosses(from, i + 1) && !(s.tokenStart()[from] && s.tail()[i] <= 1)) continue;
            if (negated(node, s, from, i + 1)) continue;
            best = i + 1;
        }
        return best;
    }

    private static boolean negated(Node node, Sanitized s, int start, int end) {
        for (var negative : node.negatives)
            if (negative.surrounds(s, start, end)) return true;
        return false;
    }

    private static int slot(char c) {
        if (c >= 'a' && c <= 'z') return c - 'a';
        if (c >= '0' && c <= '9') return 26 + c - '0';
        return -1;
    }
}
