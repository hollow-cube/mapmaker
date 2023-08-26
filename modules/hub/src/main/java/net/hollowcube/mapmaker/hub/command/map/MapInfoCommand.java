package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapInfoCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PUBLISHED_ID);

    public MapInfoCommand() {
        super("info");

        addSyntax(wrap(this::showInfoAboutCurrent));
        addSyntax(wrap(this::showInfoAboutTarget), mapArg);
    }

    private void showInfoAboutCurrent(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("this is not implemented yet, eventually it will show info about your current map.");
    }

    private void showInfoAboutTarget(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) return;

        printMapInfo(player, map);
    }

    private void printMapInfo(@NotNull Player player, @NotNull MapData map) {
        player.sendMessage("Map Info:");
        player.sendMessage("  ID: " + map.id());
        player.sendMessage("  Name: " + map.name());
    }

}
