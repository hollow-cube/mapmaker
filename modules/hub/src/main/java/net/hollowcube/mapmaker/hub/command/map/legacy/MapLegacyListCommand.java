package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MapLegacyListCommand extends BaseHubCommand {
    private final Argument<UUID> playerArg = ArgumentType.UUID("player"); //todo support names too

    public MapLegacyListCommand() {
        super("list");

        addSyntax(wrap(this::listMapsForSelf));
        addSyntax(wrap(this::listMapsForOther), playerArg);
    }

    private void listMapsForSelf(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("listMapsForSelf");
    }

    private void listMapsForOther(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("listMapsForOther");
    }

}
