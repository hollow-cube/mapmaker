package net.hollowcube.luau.ap.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DocContent(@NotNull String text, @Nullable String codeSample, @Nullable String url) {

    public static @Nullable DocContent parse(@Nullable String javadoc) {
        if (javadoc == null) return null;

        int prefix = Integer.MAX_VALUE;
        for (var line : javadoc.split("\n")) {
            if (line.isBlank()) continue;

            // Find the first non-whitespace character
            int i = 0;
            while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
            if (i == line.length()) continue;
            prefix = Math.min(prefix, i);
        }

        String text = null;

        StringBuilder builder = new StringBuilder();
        for (var line : javadoc.split("\n")) {
            if (line.length() >= prefix) line = line.substring(prefix);

            if (line.startsWith("<code>")) {
                text = builder.toString();
                builder.setLength(0);
                continue;
            } else if (line.startsWith("</code>")) {
                break;
            }

            builder.append(line).append('\n');
        }
        if (text == null) {
            text = builder.toString();
            builder.setLength(0);
        }

        return new DocContent(text, builder.toString(), null);
    }

    public DocContent {
        text = text.trim();
        codeSample = codeSample == null ? null : codeSample.trim();
        codeSample = codeSample == null || codeSample.isEmpty() ? null : codeSample;
    }
}
