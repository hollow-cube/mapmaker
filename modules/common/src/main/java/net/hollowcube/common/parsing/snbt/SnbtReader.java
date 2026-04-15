package net.hollowcube.common.parsing.snbt;

import it.unimi.dsi.fastutil.chars.CharPredicate;
import net.hollowcube.common.parsing.ParsingException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class SnbtReader {

    private final String string;
    private final int maxDepth;

    private int cursor = 0;
    private int depth = 0;

    public SnbtReader(@NotNull String string, int maxDepth) {
        this.string = string;
        this.maxDepth = maxDepth;
    }

    public int cursor() {
        return this.cursor;
    }

    public char peek() {
        return peek(0);
    }

    public char peek(int offset) {
        if (this.cursor + offset >= this.string.length()) return '\0';
        return this.string.charAt(this.cursor + offset);
    }

    public char read() {
        if (this.cursor >= this.string.length()) throw new ParsingException(this.cursor, "Cannot read past end of string");
        return this.string.charAt(this.cursor++);
    }

    public String read(int amount) {
        if (this.cursor + amount > this.string.length()) throw new ParsingException(this.cursor, "Cannot read past end of string");
        String result = this.string.substring(this.cursor, this.cursor + amount);
        this.cursor += amount;
        return result;
    }

    public String read(CharPredicate predicate) {
        int start = this.cursor;
        while (this.cursor < this.string.length() && predicate.test(this.string.charAt(this.cursor))) {
            this.cursor++;
        }
        return this.string.substring(start, this.cursor);
    }

    public char require(char... expected) {
        char c = read();
        for (char e : expected) {
            if (c == e) return c;
        }
        throw new ParsingException(this.cursor - 1, "Expected one of " + Arrays.toString(expected) + " but got '" + c + "'");
    }

    public void require(String expected) {
        if (this.cursor + expected.length() >= this.string.length()) throw new ParsingException(this.cursor, "Expected '" + expected + "' but got end of string");
        var actual = this.string.substring(this.cursor, this.cursor + expected.length());
        if (!actual.equals(expected)) {
            throw new ParsingException(this.cursor - 1, "Expected '" + expected + "' but got '" + actual + "'");
        }
        this.cursor += expected.length();
    }

    public Scope scope() {
        return new Scope();
    }

    public class Scope implements AutoCloseable {

        private Scope() {
            SnbtReader.this.depth++;
            if (SnbtReader.this.depth > SnbtReader.this.maxDepth) {
                throw new ParsingException(SnbtReader.this.cursor, "Exceeded maximum depth of " + SnbtReader.this.maxDepth);
            }
        }

        @Override
        public void close() {
            if (SnbtReader.this.depth == 0) throw new IllegalStateException("Cannot pop from empty stack");
            SnbtReader.this.depth--;
        }
    }
}
