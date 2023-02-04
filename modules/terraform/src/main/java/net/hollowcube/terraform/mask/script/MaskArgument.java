package net.hollowcube.terraform.mask.script;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.utils.StringUtils;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaskArgument {

    private static class MyStringArg extends Argument<String> {

        private static final char BACKSLASH = '\\';
        private static final char DOUBLE_QUOTE = '"';
        private static final char QUOTE = '\'';

        public static final int QUOTE_ERROR = 1;

        public MyStringArg(String id) {
            super(id, true);
        }

        @NotNull
        @Override
        public String parse(@NotNull String input) throws ArgumentSyntaxException {
            return staticParse(input);
        }

        @Override
        public String parser() {
            return "brigadier:string";
        }

        @Override
        public byte @Nullable [] nodeProperties() {
            return BinaryWriter.makeArray(packetWriter -> {
                packetWriter.writeVarInt(2); // Quotable phrase
            });
        }

        /**
         * @deprecated use {@link Argument#parse(Argument)}
         */
        @Deprecated
        public static String staticParse(@NotNull String input) throws ArgumentSyntaxException {
            // Return if not quoted
            if (!input.contains(String.valueOf(DOUBLE_QUOTE)) &&
                    !input.contains(String.valueOf(QUOTE)) &&
                    !input.contains(StringUtils.SPACE)) {
                return input;
            }

            // Check if value start and end with quote
            final char first = input.charAt(0);
            final char last = input.charAt(input.length() - 1);
            final boolean quote = input.length() >= 2 &&
                    first == last && (first == DOUBLE_QUOTE || first == QUOTE);
            if (!quote)
                throw new ArgumentSyntaxException("String argument needs to start and end with quotes", input, QUOTE_ERROR);

            // Remove first and last characters (quotes)
            input = input.substring(1, input.length() - 1);

            // Verify backslashes
            for (int i = 1; i < input.length(); i++) {
                final char c = input.charAt(i);
                if (c == first) {
                    final char lastChar = input.charAt(i - 1);
                    if (lastChar != BACKSLASH) {
                        throw new ArgumentSyntaxException("Non-escaped quote", input, QUOTE_ERROR);
                    }
                }
            }

            return StringUtils.unescapeJavaString(input);
        }

        @Override
        public String toString() {
            return String.format("String<%s>", getId());
        }

    }

    public static final Argument<String> MASK = new MyStringArg("mask")
            .setSuggestionCallback((sender, context, suggestion) -> {

                // Blocks
                // Prefix operators (!, >, <)
                // Infix operators (/, |)

                // Split into Parser classes which each can handle a specific type of input
                // the parser class knows how to provide suggestions

                int start = suggestion.getStart() - 1;
                int length = suggestion.getLength();

//                System.out.println(String.format("input='%s', start=%d, length=%d", suggestion.getInput(), suggestion.getStart(), suggestion.getLength()));
                var index = suggestion.getLength();
                var input = suggestion.getInput().substring(start, start + length);
                if (input.length() == 1 && input.charAt(0) == '\u0000') {
                    index = 0;
                    input = "";
                }
                System.out.println(index + " " + input);

                var root = new Parser(input).parse();
                var child = root.getChildAt(index);
                if (child == null) {
                    System.out.println("NULL CHILD");
                    return;
                }

                var completions = child.complete(index);
                if (completions == null) {
                    System.out.println("NULL COMPLETIONS " + child);
                    return;
                }

                System.out.println("Got completions: " + completions);
                for (var completion : completions) {
                    suggestion.addEntry(new SuggestionEntry(completion));
                }

                suggestion.setStart(suggestion.getStart() + child.start());
//                suggestion.setLength();
//                suggestion.setStart(suggestion.getStart() + 1);

//                suggestion.addEntry(new SuggestionEntry("test 123"));
            });
}
