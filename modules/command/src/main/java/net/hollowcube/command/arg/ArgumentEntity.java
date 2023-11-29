package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArgumentEntity extends Argument<EntityFinder> {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    //todo this argument needs a ton of work!
    private boolean onlySingleEntity = false;
    private boolean onlyPlayers = false;

    ArgumentEntity(@NotNull String id) {
        super(id);
    }

    public @NotNull ArgumentEntity singleEntity(boolean singleEntity) {
        this.onlySingleEntity = singleEntity;
        return this;
    }

    public @NotNull ArgumentEntity onlyPlayers(boolean onlyPlayers) {
        this.onlyPlayers = onlyPlayers;
        return this;
    }

    @Override
    public @NotNull ParseResult<EntityFinder> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        try {
            var input = reader.readWord(WordType.GREEDY);
            return new ParseSuccess<>(net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity
                    .staticParse(sender, input, onlySingleEntity, onlyPlayers));
        } catch (ArgumentSyntaxException ignored) {
            return new ParsePartial<>();
        }
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        // for now, just suggest matching players
        var input = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        for (var player : CONNECTION_MANAGER.getPlayers(ConnectionState.PLAY)) {
            if (player.getUsername().toLowerCase(Locale.ROOT).startsWith(input)) {
                suggestion.add(player.getUsername());
            }
        }
    }
}
