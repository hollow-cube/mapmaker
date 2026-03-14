package net.hollowcube.mapmaker.command.arg;

import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.misc.Emoji;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;

public class MessageArgument extends Argument<String> {

    private static final CharSet EMOJI_CHARS = OpUtils.build(new CharArraySet(), set -> {
        for (Emoji emoji : Emoji.values()) {
            for (char c : emoji.name().toCharArray()) {
                set.add(c);
            }
        }
    });

    MessageArgument(String id) {
        super(id);
    }

    @Override
    public ParseResult<String> parse(CommandSender sender, StringReader reader) {
        return success(reader.readRemaining());
    }

    @Override
    public ArgumentParserType argumentType() {
        return ArgumentParserType.STRING;
    }

    @Override
    public void properties(NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, 2);
    }

    @Override
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        int index = getEmojiIndex(raw);

        if (index != -1) {
            String toEnd = raw.substring(index + 1);
            suggestion.setStart(suggestion.getStart() + index);

            for (Emoji emoji : Emoji.values()) {
                if (emoji.name().startsWith(toEnd)) {
                    suggestion.add(":" + emoji.name() + ":");
                }
            }
        }
    }

    private static int getEmojiIndex(String message) {
        int index = -1;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == ':') {
                index = index == -1 ? i : -1;
            } else if (!EMOJI_CHARS.contains(c) && index != -1) {
                index = -1;
            }
        }
        return index;
    }
}
