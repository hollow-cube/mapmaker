package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.gui.map.browser.MapBrowserView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlayCommand extends CommandDsl {

    private final Argument<@Nullable MapData> mapArg;

    private final ApiClient api;
    private final MapService mapService;
    private final SessionManager sessionManager;
    private final ServerBridge bridge;

    public PlayCommand(
        @NotNull ApiClient api,
        @NotNull MapService mapService,
        @NotNull SessionManager sessionManager,
        @NotNull ServerBridge bridge
    ) {
        super("play");
        this.api = api;
        this.mapService = mapService;
        this.sessionManager = sessionManager;
        this.bridge = bridge;

        mapArg = CoreArgument.Map("map", mapService)
            .description("The ID of the map to play");

        category = CommandCategories.SOCIAL;
        description = "Teleport to a map, resuming your progress if you have any";
        examples = List.of("/play 123-456-789");

        addSyntax(playerOnly(this::handleDefault));
        addSyntax(playerOnly(this::joinTargetMap), mapArg);
    }

    private void handleDefault(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new MapBrowserView(api, mapService, bridge));
    }

    private void joinTargetMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) {
            player.sendMessage(Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }

        var currentMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);
        if (currentMap != null && currentMap.id().equals(map.id())) {
            player.sendMessage(Component.translatable("command.play.already_playing", currentMap.settings().getNameComponent()));
            return;
        }

        if (!map.isPublished()) {
            player.sendMessage(Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }

        bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.PLAYING, "play_command");
    }

}
