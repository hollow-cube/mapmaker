package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.Permission;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class MapEditCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;

    private final ServerBridge bridge;

    public MapEditCommand(@NotNull MapClient maps, @NotNull ServerBridge bridge) {
        super("edit");
        this.bridge = bridge;

        description = "Edit a map world (forced)";
        examples = List.of("/map edit 123-456-789", "/map edit a12345bc-67de-8f91-ghij-2345k6l78912");
        mapArg = CoreArgument.Map("map", maps)
            .description("The ID of the map to edit");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleForceEditMap), mapArg);
    }

    @Blocking
    private void handleForceEditMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING, "staff_edit_map");
    }
}
