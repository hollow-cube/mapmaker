package net.hollowcube.command.util;

import org.jetbrains.annotations.NotNull;

public final class StringReader {
    private final String string;
    private int pos = 0;

    public StringReader(@NotNull String string) {
        this.string = string;
    }

    public int mark() {
        return pos;
    }

    public void restore(int mark) {
        this.pos = mark;
    }

    public int pos() {
        var pos = this.pos;
        while (pos < string.length() && string.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    public boolean canRead() {
        return pos < string.length();
    }

    public char read() {
        if (!canRead()) return 0;
        return string.charAt(pos++);
    }

    public @NotNull String readWord(@NotNull WordType type) {
        skipWhitespace();

        int end = pos;
        while (end < string.length() && type.test(string.charAt(end))) {
            end++;
        }

        var word = string.substring(pos, end);
        pos = end;
        return word;
    }

    public @NotNull String readRemaining() {
        skipWhitespace();
        var remaining = string.substring(pos);
        pos = string.length();
        return remaining;
    }

    private void skipWhitespace() {
        while (pos < string.length() && string.charAt(pos) == ' ') {
            pos++;
        }
    }

}
