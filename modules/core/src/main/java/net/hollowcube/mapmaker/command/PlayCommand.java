package net.hollowcube.mapmaker.command;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayCommand extends Command {
    private static final Argument<@NotNull String> mapIDArg = ArgumentType.String("Map ID");

    private final MapService mapService;
    private final HubToMapBridge hubToMapBridge;

    public PlayCommand(@NotNull MapService mapService, @NotNull HubToMapBridge hubToMapBridge) {
        super("play", "pl", "enter", "e");

        this.mapService = mapService;
        this.hubToMapBridge = hubToMapBridge;

        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.translatable("command.play.usage")));

        addSyntax(this::playMap, mapIDArg);
    }

    private void playMap(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("generic.player_only"));
            return;
        }

        String publishedIDArg = context.get("Map ID");

        try {
            try {
                long publishedID = parsePublishedID(publishedIDArg);
                var mapData = mapService.getMapByPublishedId(player.getUsername(), publishedID);
                joinMap(player, mapData);
            } catch (MapService.NotFoundError e) {
                sender.sendMessage(Component.translatable("command.play.invalid_ID", Component.text(publishedIDArg)));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.translatable("command.play.wrong_format", Component.text(publishedIDArg)));
        }
    }

    private long parsePublishedID(String idString) {
        idString = idString.replace("-", "");

        while (idString.length() > 1 && idString.startsWith("0")) {
            idString = idString.substring(1);
        }

        return Long.parseLong(idString);

    }

    private void joinMap(@NotNull Player player, @NotNull MapData mapData) {
        hubToMapBridge.joinMap(player, mapData.id(), HubToMapBridge.JoinMapState.PLAYING);
    }
}
