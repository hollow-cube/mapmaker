package net.hollowcube.common.parsing.snbt;

import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.BinaryTag;

public class Snbt {

    private static final int DEFAULT_MAX_DEPTH = 64;

    public static BinaryTag parse(String input) throws ParsingException {
        return parse(new SnbtReader(input, DEFAULT_MAX_DEPTH));
    }

    public static BinaryTag parse(SnbtReader reader) throws ParsingException {
        return switch (reader.peek()) {
            case '{' -> SnbtObject.parseObject(reader);
            case '[' -> SnbtArray.parseArray(reader);
            case 't', 'T', 'f', 'F' -> SnbtMisc.parseBoolean(reader);
            case '\"', '\'' -> SnbtString.parse(reader);
            case '-', '+', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> SnbtNumber.parse(reader);
            default -> {
                if (SnbtString.ALPHA_PREDICATE.test(reader.peek())) {
                    var string = SnbtString.parseUnquoted(reader);
                    var operation = SnbtMisc.parseOperation(reader, string.value());
                    if (operation != null) {
                        yield operation;
                    }
                    yield string;
                }
                throw new ParsingException(reader.cursor(), "Unexpected character: '" + reader.peek() + "'");
            }
        };
    }
}
