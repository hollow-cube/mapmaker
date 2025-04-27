package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ArgumentGreedyString extends Argument<String> {

    ArgumentGreedyString(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return success(reader.readRemaining());
    }
}
