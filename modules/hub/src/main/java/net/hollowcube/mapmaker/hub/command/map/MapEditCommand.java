package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapEditCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PERSONAL_WORLD | MASK_PUBLISHED_ID);

    private final HubHandler handler;

    public MapEditCommand(@NotNull HubHandler handler) {
        super("edit");
        this.handler = handler;

        addSyntax(wrap(this::editMap), mapArg);
    }

    private void editMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) return;

        player.sendMessage("Sending you to your map :O");
        handler.editMap(player, map.id());
    }

}
