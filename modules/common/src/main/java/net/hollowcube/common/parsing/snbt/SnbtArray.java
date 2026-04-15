package net.hollowcube.common.parsing.snbt;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.*;

import java.util.function.Function;

public class SnbtArray {

    static BinaryTag parseArray(SnbtReader reader) throws ParsingException {
        try (var _ = reader.scope()) {
            reader.require('[');
            reader.read(SnbtMisc.WHITESPACE_PREDICATE);
            var type = getArrayType(reader);
            if (type == '\0') {
                var output = ListBinaryTag.builder();
                parseArray(reader, () -> output.add(Snbt.parse(reader)));
                return output.build();
            } else if (type == 'B' || type == 'b') {
                var output = new ByteArrayList();
                parseArray(reader, () -> output.add(SnbtMisc.parseBooleanOrByte(reader).value()));
                return ByteArrayBinaryTag.byteArrayBinaryTag(output.toByteArray());
            } else if (type == 'I' || type == 'i') {
                var output = new IntArrayList();
                parseArray(reader, () -> output.add(expect(reader, SnbtNumber::parse, IntBinaryTag.class).value()));
                return IntArrayBinaryTag.intArrayBinaryTag(output.toIntArray());
            } else if (type == 'L' || type == 'l') {
                var output = new LongArrayList();
                parseArray(reader, () -> output.add(expect(reader, SnbtNumber::parse, LongBinaryTag.class).value()));
                return LongArrayBinaryTag.longArrayBinaryTag(output.toLongArray());
            }
            throw new ParsingException(reader.cursor(), "Expected array type but got '" + type + "'");
        }
    }

    private static char getArrayType(SnbtReader reader) throws ParsingException {
        var type = reader.peek();
        var offset = 0;
        while (SnbtMisc.WHITESPACE_PREDICATE.test(reader.peek(offset))) {
            offset++;
        }
        if (reader.peek(offset) == ';') {
            reader.read(offset);
            reader.require(';');
            return type;
        }
        return '\0';
    }

    private static <T extends BinaryTag> T expect(SnbtReader reader, Function<SnbtReader, BinaryTag> parser, Class<T> expected) {
        var tag = parser.apply(reader);
        if (expected.isInstance(tag)) {
            return expected.cast(tag);
        } else {
            throw new ParsingException(reader.cursor(), "Expected " + expected.getSimpleName() + " but got " + tag.getClass().getSimpleName());
        }
    }

    private static void parseArray(SnbtReader reader, Runnable elementReader) throws ParsingException {
        reader.read(SnbtMisc.WHITESPACE_PREDICATE);

        while (reader.peek() != ']') {
            elementReader.run();
            reader.read(SnbtMisc.WHITESPACE_PREDICATE);
            if (reader.peek() == ',') {
                reader.read();
                reader.read(SnbtMisc.WHITESPACE_PREDICATE);
            } else if (reader.peek() == ']') {
                break;
            } else {
                throw new ParsingException(reader.cursor(), "Expected ',' or ']' but got '" + reader.peek() + "'");
            }
        }
        reader.require(']');
    }
}
