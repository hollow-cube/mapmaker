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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class MapPlayCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;
    private final Argument<String> isolateArg;

    private final ServerBridge bridge;

    public MapPlayCommand(@NotNull MapClient maps, @NotNull ServerBridge bridge) {
        super("play");
        this.bridge = bridge;

        description = "Play a map (forced)";
        examples = List.of("/map play 123-456-789", "/map play a12345bc-67de-8f91-ghij-2345k6l78912");
        mapArg = CoreArgument.Map("map", maps)
            .description("The ID of the map to play");
        isolateArg = Argument.GreedyString("isolate");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleForcePlayMap), mapArg);
        addSyntax(playerOnly(this::handleForcePlayMap), mapArg, isolateArg);
    }

    private void handleForcePlayMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var isolateOverride = context.get(isolateArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }

        bridge.joinMap(player, new ServerBridge.JoinConfig(map.id(), ServerBridge.JoinMapState.PLAYING, "staff_play_map", isolateOverride));
    }
}
