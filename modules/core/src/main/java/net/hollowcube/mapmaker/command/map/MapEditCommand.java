package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MapEditCommand extends CommandDsl {
    private final Argument<@NotNull MapData> mapArg;

    private final MapService mapService;
    private final ServerBridge bridge;

    public MapEditCommand(@NotNull MapService mapService, @NotNull PermManager permManager, @NotNull ServerBridge bridge) {
        super("edit");
        this.mapService = mapService;
        this.bridge = bridge;

        description = "Edit a map world (forced)";
        examples = List.of("/map edit 123-456-789", "/map edit a12345bc-67de-8f91-ghij-2345k6l78912");
        mapArg = CoreArgument.PlayableMap("map", mapService)
                .description("The ID of the map to edit");

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleForceEditMap), mapArg);
    }

    private void handleForceEditMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);

        bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING);
    }
}
