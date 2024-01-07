package net.hollowcube.mapmaker.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.argold.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.command.util.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayCommand extends CommandDsl {
    private final Argument<MapData> mapArg;

    private final ServerBridge bridge;

    @Inject
    public PlayCommand(@NotNull MapService mapService, @NotNull ServerBridge bridge) {
        super("play");
        this.bridge = bridge;

        category = CommandCategory.SOCIAL;
        description = "Play a map by ID or search for a map";
        mapArg = CoreArgument.PlayableMap("map", mapService)
                .errorHandler(this::mapArgErrorHandler)
                .doc("The ID of the map to play");

        addSyntax(playerOnly(this::joinTargetMap), mapArg);
        setDefaultExecutor(playerOnly(this::handleDefault));
    }

    private void handleDefault(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("todo this should open the map search gui");
        //var server = HubWorld.fromInstance(player.getInstance()).server(); TODO properly
        //server.newOpenGUI(player, context -> new PlayMapsView(context.with(Map.of("query", ""))));
    }

    private void joinTargetMap(@NotNull Player player, @NotNull CommandContext context) {
        bridge.joinMap(player, context.get(mapArg).id(), HubToMapBridge.JoinMapState.PLAYING);
    }

    private void mapArgErrorHandler(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage(Component.translatable("command.play.invalid_id", Component.text(context.getRaw(mapArg))));
    }
}
