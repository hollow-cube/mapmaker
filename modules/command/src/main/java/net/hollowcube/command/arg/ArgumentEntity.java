package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.Locale;

public class ArgumentEntity extends Argument<EntityFinder> {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    //todo this argument needs a ton of work!
    private boolean onlySingleEntity = false;
    private boolean onlyPlayers = false;
    private boolean sameWorld = false;

    ArgumentEntity(String id) {
        super(id);
    }

    public ArgumentEntity singleEntity(boolean singleEntity) {
        this.onlySingleEntity = singleEntity;
        return this;
    }

    public ArgumentEntity onlyPlayers(boolean onlyPlayers) {
        this.onlyPlayers = onlyPlayers;
        return this;
    }

    public ArgumentEntity sameWorld(boolean sameWorld) {
        this.sameWorld = sameWorld;
        return this;
    }

    @Override
    public ParseResult<EntityFinder> parse(CommandSender sender, StringReader reader) {
        try {
            var input = reader.readWord(WordType.GREEDY);
            return success(net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity
                    .staticParse(sender, input, onlySingleEntity, onlyPlayers));
        } catch (ArgumentSyntaxException ignored) {
            return partial();
        }
    }

    @Override
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);

        // for now, just suggest matching players
        for (var player : CONNECTION_MANAGER.getOnlinePlayers()) {
            if (sameWorld && sender instanceof Player p && !p.getInstance().equals(player.getInstance()))
                continue;
            if (player.getUsername().toLowerCase(Locale.ROOT).startsWith(raw)) {
                suggestion.add(player.getUsername());
            }
        }
    }
}
