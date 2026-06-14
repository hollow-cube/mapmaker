package net.hollowcube.common.parsing.snbt;

import it.unimi.dsi.fastutil.chars.CharPredicate;
import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;

import java.util.UUID;

public class SnbtMisc {

    static final CharPredicate WHITESPACE_PREDICATE = Character::isWhitespace;

    static ByteBinaryTag parseBoolean(SnbtReader reader) throws ParsingException {
        return switch (reader.peek()) {
            case 't', 'T' -> {
                reader.require("true");
                yield ByteBinaryTag.byteBinaryTag((byte) 1);
            }
            case 'f', 'F' -> {
                reader.require("false");
                yield ByteBinaryTag.byteBinaryTag((byte) 0);
            }
            default -> throw new ParsingException(reader.cursor(), "Expected 'true' or 'false'");
        };
    }

    static ByteBinaryTag parseBooleanOrByte(SnbtReader reader) throws ParsingException {
        if (reader.peek() == 't' || reader.peek() == 'T' || reader.peek() == 'f' || reader.peek() == 'F') {
            return SnbtMisc.parseBoolean(reader);
        } else if (SnbtNumber.parse(reader) instanceof ByteBinaryTag tag) {
            return tag;
        } else {
            throw new ParsingException(reader.cursor(), "Expected boolean or byte value");
        }
    }

    static BinaryTag parseOperation(SnbtReader reader, String name) throws ParsingException {
        return switch (name) {
            case "uuid" -> {
                try {
                    reader.read(WHITESPACE_PREDICATE);
                    reader.require('(');
                    var uuid = UUID.fromString(SnbtString.parseUnquoted(reader).value());
                    reader.read(WHITESPACE_PREDICATE);
                    reader.require(')');

                    yield IntArrayBinaryTag.intArrayBinaryTag(
                        (int) (uuid.getMostSignificantBits() >> 32),
                        (int) uuid.getMostSignificantBits(),
                        (int) (uuid.getLeastSignificantBits() >> 32),
                        (int) uuid.getLeastSignificantBits()
                    );
                } catch (IllegalArgumentException e) {
                    throw new ParsingException(reader.cursor(), "Invalid UUID format");
                }
            }
            case "bool" -> {
                reader.read(WHITESPACE_PREDICATE);
                reader.require('(');
                var value = parseBooleanOrByte(reader);
                reader.read(WHITESPACE_PREDICATE);
                reader.require(')');
                yield ByteBinaryTag.byteBinaryTag(value.value() != 0 ? (byte) 1 : (byte) 0);
            }
            default -> null;
        };
    }
}
