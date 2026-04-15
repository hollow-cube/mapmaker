package net.hollowcube.common.parsing.snbt;

import it.unimi.dsi.fastutil.chars.CharPredicate;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.hollowcube.common.parsing.ParsingException;
import net.kyori.adventure.nbt.*;

public class SnbtNumber {

    private static final CharPredicate NUMERIC_PREDICATE = c -> c >= '0' && c <= '9';
    private static final CharPredicate NUMBER_SIGNEDNESS_SUFFIX_PREDICATE = CharSet.of('s', 'S', 'u', 'U')::contains;
    private static final CharPredicate INTEGER_SUFFIX_PREDICATE = NUMBER_SIGNEDNESS_SUFFIX_PREDICATE
        .or(CharSet.of('b', 'B', 's', 'S', 'i', 'I', 'l', 'L')::contains);

    private static final CharPredicate NUMBER_CHARS_PREDICATE = NUMERIC_PREDICATE
        .or(CharSet.of('-', '+', '.', '_', 'e', 'E')::contains)
        .or(CharSet.of('b', 'B', 's', 'S', 'i', 'I', 'l', 'L', 'f', 'F', 'd', 'D')::contains);

    static NumberBinaryTag parse(SnbtReader reader) throws ParsingException {
        var sign = reader.peek();
        if (sign == '-' || sign == '+') {
            reader.read();
        } else {
            sign = '+';
        }

        if (reader.peek() == '0' && (reader.peek(1) == 'b' || reader.peek(1) == 'B' || reader.peek(1) == 'x' || reader.peek(1) == 'X')) {
            reader.read();
            return parseHexOrBin(reader, sign == '-');
        } else {
            var input = reader.read(NUMBER_CHARS_PREDICATE);
            if (input.isEmpty()) {
                throw new ParsingException(reader.cursor(), "Expected a number but got nothing");
            }

            var lastChar = Character.toLowerCase(input.charAt(input.length() - 1));

            var signness = '\0';
            var type = 'i';

            if (lastChar != 'u') {
                if (lastChar == 'b' || lastChar == 's' || lastChar == 'i' || lastChar == 'l' || lastChar == 'f' || lastChar == 'd') {
                    type = lastChar;

                    if (input.length() == 1) {
                        throw new ParsingException(reader.cursor(), "Invalid number format: '" + input + "' is not a valid number");
                    }

                    var secondLastChar = Character.toLowerCase(input.charAt(input.length() - 2));
                    if (secondLastChar == 'u') {
                        signness = 'u';
                        input = input.substring(0, input.length() - 2);
                    } else if (secondLastChar == 's') {
                        signness = 's';
                        input = input.substring(0, input.length() - 2);
                    } else {
                        input = input.substring(0, input.length() - 1);
                    }

                    if (signness != '\0' && input.length() == 2) {
                        throw new ParsingException(reader.cursor(), "Invalid number format: '" + input + "' is not a valid number");
                    }
                }
            } else {
                signness = 'u';
                input = input.substring(0, input.length() - 1);
            }

            if (type == 'f' || type == 'd') {
                if (signness != '\0') {
                    throw new ParsingException(reader.cursor(), "Floating point numbers cannot have a signedness suffix");
                } else if (input.equals(".")) {
                    throw new ParsingException(reader.cursor(), "Invalid floating point number format: '.' is not a valid number");
                }
                input = input.startsWith(".") ? "0" + input : input;
                input = input.endsWith(".") ? input + "0" : input;

                try {
                    return type == 'f' ? FloatBinaryTag.floatBinaryTag(Float.parseFloat(input)) : DoubleBinaryTag.doubleBinaryTag(Double.parseDouble(input));
                } catch (NumberFormatException e) {
                    throw new ParsingException(reader.cursor(), "Invalid floating point number format: " + e.getMessage());
                }
            } else {
                try {
                    return switch (type) {
                        case 'b' -> ByteBinaryTag.byteBinaryTag(signness == 'u' ? parseUnsignedByte(input, 10) : Byte.parseByte(input));
                        case 's' -> ShortBinaryTag.shortBinaryTag(signness == 'u' ? parseUnsignedShort(input, 10) : Short.parseShort(input));
                        case 'i' -> IntBinaryTag.intBinaryTag(signness == 'u' ? Integer.parseUnsignedInt(input) : Integer.parseInt(input));
                        case 'l' -> LongBinaryTag.longBinaryTag(signness == 'u' ? Long.parseUnsignedLong(input) : Long.parseLong(input));
                        default -> throw new IllegalStateException("Unreachable");
                    };
                } catch (NumberFormatException e) {
                    throw new ParsingException(reader.cursor(), "Invalid number format: " + e.getMessage());
                }
            }
        }
    }

    private static NumberBinaryTag parseHexOrBin(SnbtReader reader, boolean negative) throws ParsingException {
        var type = reader.read();
        var binary = type == 'b' || type == 'B';
        var hex = type == 'x' || type == 'X';

        // Special case due to the fact that 0b isnt a valid binary number but is a valid byte
        if (binary && !NUMERIC_PREDICATE.test(reader.peek())) {
            return ByteBinaryTag.byteBinaryTag((byte) 0);
        } else if (!hex && !binary) {
            throw new ParsingException(reader.cursor(), "Expected 'b', 'B', 'x', or 'X' after leading zero but got '" + type + "'");
        }

        var output = new StringBuilder();
        var underscore = false;

        loop:
        while (true) {
            switch (reader.peek()) {
                case '0', '1' -> {
                    underscore = false;
                    output.append(reader.read());
                }
                case '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F' -> {
                    if (binary) throw new ParsingException(reader.cursor(), "Invalid binary digit: '" + reader.read() + "'");
                    underscore = false;
                    output.append(reader.read());
                }
                case '_' -> {
                    if (output.isEmpty()) throw new ParsingException(reader.cursor(), "Number cannot start with an underscore");
                    underscore = true;
                    reader.read();
                }
                default -> {
                    if (underscore) throw new ParsingException(reader.cursor(), "Number cannot end with an underscore");
                    if (output.isEmpty()) throw new ParsingException(reader.cursor(), "Expected at least one digit");
                    break loop;
                }
            }
        }

        // if there is no explicit type suffix, default to int or short if it was an 's' for the sign as that means the user tried to make it a short
        var signedSuffix = NUMBER_SIGNEDNESS_SUFFIX_PREDICATE.test(reader.peek()) ? Character.toLowerCase(reader.read()) : '\0';
        var typeSuffix = INTEGER_SUFFIX_PREDICATE.test(reader.peek()) ? Character.toLowerCase(reader.read()) : '\0';

        if (negative && (signedSuffix != 's' || typeSuffix == '\0')) {
            throw new ParsingException(reader.cursor(), "Negative numbers cannot have an unsigned type suffix");
        } else if (typeSuffix == '\0') {
            typeSuffix = signedSuffix == 's' ? 's' : 'i';
        }

        return parseNumber(
            negative ? "-" + output : output.toString(),
            hex ? 16 : 2,
            typeSuffix,
            signedSuffix != 'u'
        );
    }

    private static NumberBinaryTag parseNumber(String input, int radix, char type, boolean signed) throws ParsingException {
        try {
            return switch (type) {
                case 'b' -> ByteBinaryTag.byteBinaryTag(signed ? Byte.parseByte(input, radix) : parseUnsignedByte(input, radix));
                case 's' -> ShortBinaryTag.shortBinaryTag(signed ? Short.parseShort(input, radix) : parseUnsignedShort(input, radix));
                case 'i' -> IntBinaryTag.intBinaryTag(signed ? Integer.parseInt(input, radix): Integer.parseUnsignedInt(input, radix));
                case 'l' -> LongBinaryTag.longBinaryTag(signed ? Long.parseLong(input, radix) : Long.parseUnsignedLong(input, radix));
                default -> throw new IllegalStateException("Unreachable");
            };
        } catch (NumberFormatException e) {
            throw new ParsingException(-1, "Invalid number format: " + e.getMessage());
        }
    }

    private static byte parseUnsignedByte(String number, int radix) {
        var value = Integer.parseInt(number, radix);
        if (value >> Byte.SIZE != 0) {
            throw new NumberFormatException("Value out of range for unsigned byte: " + value);
        }
        return (byte) value;
    }

    private static short parseUnsignedShort(String number, int radix) {
        var value = Integer.parseInt(number, radix);
        if (value >> Short.SIZE != 0) {
            throw new NumberFormatException("Value out of range for unsigned short: " + value);
        }
        return (short) value;
    }

}
