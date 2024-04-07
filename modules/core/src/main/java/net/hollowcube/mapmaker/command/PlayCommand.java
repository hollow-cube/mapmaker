package net.hollowcube.mapmaker.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayCommand extends CommandDsl {
    private final Argument<MapData> mapArg;

    private final MapService mapService;
    private final SessionManager sessionManager;
    private final ServerBridge bridge;

    @Inject
    public PlayCommand(@NotNull MapService mapService, @NotNull SessionManager sessionManager, @NotNull ServerBridge bridge) {
        super("play");
        this.mapService = mapService;
        this.sessionManager = sessionManager;
        this.bridge = bridge;

        mapArg = CoreArgument.PlayableMap("map", mapService)
                .description("The ID of the map to play");

        category = CommandCategories.SOCIAL;
        description = "Teleport to a map, resuming your progress if you have any";
        examples = List.of("/play 123-456-789");

        addSyntax(playerOnly(this::handleDefault));
        addSyntax(playerOnly(this::joinTargetMap), mapArg);
    }

    private void handleDefault(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("todo this should open the map search gui");
        //var server = HubWorld.fromInstance(player.getInstance()).server(); TODO properly
        //server.newOpenGUI(player, context -> new PlayMapsView(context.with(Map.of("query", ""))));
    }

    private void joinTargetMap(@NotNull Player player, @NotNull CommandContext context) {
        var mapId = context.get(mapArg).id();

        var currentMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);
        if (currentMap != null && currentMap.id().equals(mapId)) {
            player.sendMessage(Component.translatable("command.play.already_playing", currentMap.settings().getNameComponent()));
            return;
        }

        bridge.joinMap(player, mapId, ServerBridge.JoinMapState.PLAYING, "play_command");
    }

}
