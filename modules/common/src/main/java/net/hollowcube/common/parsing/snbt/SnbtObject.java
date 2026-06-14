package net.hollowcube.common.parsing.snbt;

import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class SnbtObject {

    static CompoundBinaryTag parseObject(SnbtReader reader) throws ParsingException {
        try (var _ = reader.scope()) {
            reader.require('{');
            reader.read(SnbtMisc.WHITESPACE_PREDICATE);
            var output = CompoundBinaryTag.builder();
            while (reader.peek() != '}') {
                var key = SnbtString.parse(reader).value();
                reader.read(SnbtMisc.WHITESPACE_PREDICATE);
                reader.require(':');
                reader.read(SnbtMisc.WHITESPACE_PREDICATE);
                output.put(key, Snbt.parse(reader));
                reader.read(SnbtMisc.WHITESPACE_PREDICATE);
                if (reader.peek() == ',') {
                    reader.read();
                    reader.read(SnbtMisc.WHITESPACE_PREDICATE);
                } else if (reader.peek() == '}') {
                    break;
                } else {
                    throw new ParsingException(reader.cursor(), "Expected ',' or '}' but got '" + reader.peek() + "'");
                }
            }
            reader.require('}');
            return output.build();
        }
    }
}
