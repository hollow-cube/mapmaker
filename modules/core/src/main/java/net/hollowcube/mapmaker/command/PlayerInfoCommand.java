package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public class PlayerInfoCommand extends CommandDsl {

    private static final Tag<Set<String>> PLAYER_CHANNELS = Tag.Transient("packets:player/channels");

    private final Argument<EntityFinder> playerArg = Argument.Entity("player")
            .onlyPlayers(true)
            .sameWorld(true);
    private final Argument<PlayerInfoType> typeArg = Argument.Enum("type", PlayerInfoType.class);

    public PlayerInfoCommand(@NotNull PermManager permManager) {
        super("playerinfo");

        category = CommandCategory.HIDDEN;

        setCondition(permManager.createPlatformCondition2(PlatformPerm.BAN_PLAYER));

        addSyntax(playerOnly(this::execute), playerArg, typeArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var type = context.get(typeArg);
        var target = context.get(playerArg).findFirstPlayer(player);

        if (target == null) {
            player.sendMessage("Player not found");
            return;
        }

        switch (type) {
            case CHANNELS -> sendChannels(player, target, false);
            case CHANNEL_NAMESPACES -> sendChannels(player, target, true);
            case CHEAT_REPORTS -> player.sendMessage("Currently not implemented");
        }
    }

    private void sendChannels(@NotNull Player player, @NotNull Player target, boolean namespaces) {
        Collection<String> channels = target.getTag(PLAYER_CHANNELS);
        if (channels == null) {
            player.sendMessage("No channels found");
        } else {
            channels = channels.stream().map(s -> namespaces ? s.split(":")[0] : s).distinct().toList();
            player.sendMessage("Channels: " + String.join(", ", channels));
        }
    }

    private enum PlayerInfoType {
        CHANNELS,
        CHANNEL_NAMESPACES,
        CHEAT_REPORTS,
    }

}
