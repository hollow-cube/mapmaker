package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;

public class ArgumentGreedyString extends Argument<String> {

    ArgumentGreedyString(String id) {
        super(id);
    }

    @Override
    public ParseResult<String> parse(CommandSender sender, StringReader reader) {
        return success(reader.readRemaining());
    }

    @Override
    public void properties(NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, 2);
    }

    @Override
    public ArgumentParserType argumentType() {
        return ArgumentParserType.STRING;
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
