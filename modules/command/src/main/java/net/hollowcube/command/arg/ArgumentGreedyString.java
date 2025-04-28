package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public class ArgumentGreedyString extends Argument<String> {

    ArgumentGreedyString(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return success(reader.readRemaining());
    }

    @Override
    public void properties(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, 2);
    }

    @Override
    public @NotNull ArgumentParserType argumentType() {
        return ArgumentParserType.STRING;
    }
}
