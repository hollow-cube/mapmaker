package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapAlterCommand extends BaseHubCommand {
    private final ArgumentGroup nameArg = ArgumentType.Group("name", ArgumentType.Literal("name"),
            ArgumentType.String("name"));
    private final ArgumentGroup displayArg = ArgumentType.Group("display",
            ArgumentType.Word("display").from("item", "display", "display_item"),
            ArgumentType.Resource("display", "minecraft:item"));
    private final ArgumentGroup typeArg = ArgumentType.Group("type", ArgumentType.Literal("type"),
            ArgumentType.Enum("type", MapVariant.class).setFormat(ArgumentEnum.Format.LOWER_CASED));

    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PUBLISHED_ID);
    private final ArgumentLoop<CommandContext> varargs = ArgumentType.Loop("args", nameArg, displayArg, typeArg);

    private final MapService mapService;

    public MapAlterCommand(@NotNull MapService mapService) {
        super("alter");
        this.mapService = mapService;

        addSyntax(wrap(this::alterMap), mapArg, varargs);
    }

    private void alterMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) return;

        var changes = new MapUpdateRequest();
        for (var arg : context.get(varargs)) {
            if (arg.has(nameArg)) {
                changes.setName(arg.get("name"));
            } else if (arg.has(displayArg)) {
                changes.setIcon(arg.get("display"));
            } else if (arg.has(typeArg)) {
                changes.setVariant(arg.get("type"));
            }
        }

        if (!changes.hasChanges()) {
            player.sendMessage(HubMessages.COMMAND_MAP_ALTER_NO_CHANGE);
            return;
        }

        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            mapService.updateMap(playerData.id(), map.id(), changes);
            player.sendMessage(HubMessages.COMMAND_MAP_ALTER_SUCCESS);
        } catch (Exception e) {
            //todo handle known exception cases
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(GenericMessages.COMMAND_UNKNOWN_ERROR);
        }
    }

}
