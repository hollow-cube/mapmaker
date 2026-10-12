package net.hollowcube.apiserver.chatfilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Replaces known player names with placeholders similarly to the training data to handle cases
/// like WeGay better (thanks man).
///
/// `words.txt` words are never masked to avoid generic word names (like Player) from being replaced.
public final class NameMask {

    /// This is a port of the Python version in training and Python `\w` is unicode-aware.
    private static final Pattern TOKEN = Pattern.compile("@?\\w+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String[] SUFFIXES = {"s", "es", "ed", "d", "ing", "ly", "er", "ers", "est"};

    private final String placeholder;
    private final Set<String> words;
    private volatile Set<String> names = Set.of();

    private NameMask(String placeholder, Set<String> words) {
        this.placeholder = placeholder;
        this.words = words;
    }

    public static NameMask load(String placeholder, Path words) throws IOException {
        try (var lines = Files.lines(words)) {
            return new NameMask(placeholder, Set.copyOf(lines.filter(l -> !l.isEmpty()).toList()));
        }
    }

    public void setNames(Collection<String> usernames) {
        var out = new HashSet<String>(usernames.size());
        for (var username : usernames) {
            var name = username.toLowerCase(Locale.ROOT);
            if (name.length() >= 3 && !isWord(name)) out.add(name);
        }
        names = Set.copyOf(out);
    }

    public String mask(String text) {
        var known = names;
        if (known.isEmpty()) return text;
        return TOKEN.matcher(text).replaceAll(m -> {
            var token = m.group();
            var name = token.startsWith("@") ? token.substring(1) : token;
            return Matcher.quoteReplacement(known.contains(name.toLowerCase(Locale.ROOT)) ? placeholder : token);
        });
    }

    boolean isWord(String name) {
        if (words.contains(name)) return true;
        for (var suffix : SUFFIXES) {
            if (!name.endsWith(suffix)) continue;
            var stem = name.substring(0, name.length() - suffix.length());
            if (stem.length() >= 3
                && (words.contains(stem)
                    || words.contains(stem + "e")
                    || words.contains(stem.substring(0, stem.length() - 1))))
                return true;
        }
        return false;
    }
}
