package net.hollowcube.apiserver.text;

/// What a player is allowed to put in a message at all, before anything looks at what it says.
public final class ChatText {

    /// `raw` with every character a Minecraft client cannot render, or that is only there to break
    /// the filter, removed.
    ///
    /// Keeps printable ASCII, letters from any script, the marks that accent them, and currency
    /// symbols — which [Sanitized] folds into letters, so dropping them here would let `$h1t`
    /// through as `h1t`. Everything else goes, emoji included: they arrive as replacement
    /// characters on a client and are how zalgo is written.
    ///
    /// Per code point, so an astral character is dropped whole rather than left as half a pair.
    public static String strip(String raw) {
        var out = new StringBuilder(raw.length());
        raw.codePoints().filter(ChatText::allowed).forEach(out::appendCodePoint);
        return out.toString();
    }

    private static boolean allowed(int cp) {
        if (cp >= 0x20 && cp <= 0x7E) return true;
        if (Character.isLetter(cp)) return true;
        return switch (Character.getType(cp)) {
            case Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK,
                 Character.CURRENCY_SYMBOL -> true;
            default -> false;
        };
    }

    private ChatText() {
    }
}
