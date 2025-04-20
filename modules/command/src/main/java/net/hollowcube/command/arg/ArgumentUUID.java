package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArgumentUUID extends Argument<UUID> {

    protected ArgumentUUID(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<UUID> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        try {
            return success(UUID.fromString(word));
        } catch (IllegalArgumentException e) {
            return syntaxError();
        }
    }


}
