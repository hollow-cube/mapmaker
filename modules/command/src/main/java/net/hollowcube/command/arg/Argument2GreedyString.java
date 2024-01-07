package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Argument2GreedyString extends Argument2<String> {

    Argument2GreedyString(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult2<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return success(reader.readRemaining());
    }

}
