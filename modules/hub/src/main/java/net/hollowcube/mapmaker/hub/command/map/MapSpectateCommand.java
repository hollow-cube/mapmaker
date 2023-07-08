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

public class MapSpectateCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PERSONAL_WORLD | MASK_PUBLISHED_ID);
    private final HubHandler hubHandler;
    public MapSpectateCommand(@NotNull HubHandler hubHandler) {
        super("spectate");
        this.hubHandler = hubHandler;

        addSyntax(wrap(this::spectateMap), mapArg);
    }
    private void spectateMap(@NotNull Player player, @NotNull CommandContext context) {
        MapData data = context.get(mapArg);
        hubHandler.spectateMap(player, data.id());
    }
}
