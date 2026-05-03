package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.gui.map.MapListView;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapListCommand extends CommandDsl {
    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;

    private final Argument<String> targetArg;

    public MapListCommand(@NotNull ApiClient api, @NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull ServerBridge bridge) {
        super("list");
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;

        description = "Show all the maps published by a player";

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService)
            .description("The player you want to see the maps of");

        addSyntax(playerOnly(this::execute));
        addSyntax(playerOnly(this::execute), targetArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        String targetId;
        if (!context.has(targetArg)) {
            // No target specified, use self
            targetId = PlayerData.fromPlayer(player).id();
        } else {
            // Execute for the target, if they exist.
            targetId = context.get(targetArg);
            if (targetId == null) {
                //todo this null check should be handled by the argument definition itself.
                // However, using deferred success we wont know if it was an error until too late to trigger an error handler.
                player.sendMessage("no such player: " + context.getRaw(targetArg));
                return;
            }
        }

        Panel.open(player, new MapListView.Player(api, mapService, bridge, targetId));
    }

}
