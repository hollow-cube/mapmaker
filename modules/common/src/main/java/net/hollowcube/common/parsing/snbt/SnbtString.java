package net.hollowcube.common.parsing.snbt;

import it.unimi.dsi.fastutil.chars.CharPredicate;
import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.StringBinaryTag;

import java.util.HexFormat;

public class SnbtString {

    private static final CharPredicate NUMERIC_PREDICATE = c -> c >= '0' && c <= '9';
    static final CharPredicate ALPHA_PREDICATE = c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');

    private static final CharPredicate UNICODE_CODEPOINT_PREDICATE = NUMERIC_PREDICATE
        .or(ALPHA_PREDICATE)
        .or(c -> c == '-' || c == ' ');
    private static final CharPredicate UNQUOTED_STRING_PREDICATE = NUMERIC_PREDICATE
        .or(ALPHA_PREDICATE)
        .or(c -> c == '_' || c == '-' || c == '.' || c == '+');

    static StringBinaryTag parse(SnbtReader reader) throws ParsingException {
        if (reader.peek() == '\"' || reader.peek() == '\'') {
            return parseQuoted(reader);
        } else if (ALPHA_PREDICATE.test(reader.peek())) {
            return parseUnquoted(reader);
        }
        throw new ParsingException(reader.cursor(), "Unexpected character at start of string: '" + reader.read() + "'");
    }

    static StringBinaryTag parseQuoted(SnbtReader reader) throws ParsingException {
        var quote = reader.read();
        if (quote != '\"' && quote != '\'') {
            throw new ParsingException(reader.cursor() - 1, "Expected '\"' or '\\'' but got '" + quote + "'");
        }

        var output = new StringBuilder();
        while (true) {
            char c = reader.read();
            if (c == '\\') {
                var next = reader.read();
                switch (next) {
                    case 'b' -> output.append('\b');
                    case 'f' -> output.append('\f');
                    case 'n' -> output.append('\n');
                    case 'r' -> output.append('\r');
                    case 's' -> output.append(' ');
                    case 't' -> output.append('\t');
                    case '\\' -> output.append('\\');
                    case '\'' -> output.append('\'');
                    case '\"' -> output.append('\"');
                    case 'x', 'u', 'U' -> {
                        var cursor = reader.cursor();
                        var length = switch (next) {
                            case 'x' -> 2;
                            case 'u' -> 4;
                            case 'U' -> 8;
                            default -> throw new IllegalStateException("Unreachable");
                        };
                        var codepoint = HexFormat.fromHexDigits(reader.read(length));
                        if (Character.isValidCodePoint(codepoint)) {
                            output.appendCodePoint(codepoint);
                        } else {
                            throw new ParsingException(cursor, "Invalid Unicode code point: " + Integer.toHexString(codepoint));
                        }
                    }
                    case 'N' -> {
                        var cursor = reader.cursor();
                        var name = reader.read(UNICODE_CODEPOINT_PREDICATE);
                        try {
                            output.appendCodePoint(Character.codePointOf(name));
                        } catch (IllegalArgumentException e) {
                            throw new ParsingException(cursor, "Invalid Unicode character name: " + name);
                        }
                    }
                }
            } else if (c == quote) {
                break;
            } else {
                output.append(c);
            }
        }
        return StringBinaryTag.stringBinaryTag(output.toString());
    }

    static StringBinaryTag parseUnquoted(SnbtReader reader) throws ParsingException {
        var start = reader.cursor();
        var value = reader.read(UNQUOTED_STRING_PREDICATE);
        if (value.isEmpty()) {
            throw new ParsingException(start, "Expected unquoted string");
        }
        return StringBinaryTag.stringBinaryTag(value);
    }
}
