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
        super("play", "enter", "e");

        this.mapService = mapService;
        this.hubToMapBridge = hubToMapBridge;

        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.translatable("command.play.usage")));
        System.out.println("1");

        addSyntax(this::playMap, mapIDArg);
    }

    private void playMap(@NotNull CommandSender sender, @NotNull CommandContext context) {
        System.out.println("2");
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.translatable("generic.player_only"));
            return;
        }
        System.out.println("3");
        String publishedIDArg = context.get("Map ID");

        try {
            Player player = ((Player) sender);
            System.out.println("4");
            try {
                long publishedID = Long.parseLong(publishedIDArg);
                var mapData = mapService.getMapByPublishedId(player.getUsername(), publishedID);
                joinMap(player, mapData);
                System.out.println("5");
            } catch (MapService.NotFoundError e) {
                sender.sendMessage(Component.translatable("command.play.invalid_ID", Component.text(publishedIDArg)));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.translatable("command.play.wrong_format", Component.text(publishedIDArg)));
            System.out.println("6");
        }
        System.out.println("7");
    }

    private void joinMap(@NotNull Player player, @NotNull MapData mapData) {
        hubToMapBridge.joinMap(player, mapData.toString(), false, false);
        System.out.println("8");
    }
}
