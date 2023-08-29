package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MapLegacyListCommand extends BaseHubCommand {
    private final Argument<UUID> playerArg = ArgumentType.UUID("player"); //todo support names too, and offline players -- this is a weird query tho need to get a list of all potential legacy players

    private final MapService mapService;

    public MapLegacyListCommand(@NotNull MapService mapService) {
        super("list");
        this.mapService = mapService;

        addSyntax(wrap(this::listMaps));
        addSyntax(wrap(this::listMaps), playerArg);
    }

    private void listMaps(@NotNull Player player, @NotNull CommandContext context) {
        var target = player.getUuid().toString();
        if (context.has(playerArg)) target = context.get(playerArg).toString();

        var playerData = PlayerDataV2.fromPlayer(player);
        var legacyMaps = mapService.getLegacyMaps(playerData.id(), target);

        player.sendMessage("listMapsFor " + target + ": " + legacyMaps);
    }

}
