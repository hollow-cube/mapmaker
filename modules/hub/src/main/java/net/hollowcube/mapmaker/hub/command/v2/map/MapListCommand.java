package net.hollowcube.mapmaker.hub.command.v2.map;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapListCommand extends Command {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final Argument<String> targetArg;

    private final MapService mapService;

    public MapListCommand(
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        super("list");
        this.mapService = mapService;

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService)
                .doc("The player to list maps for", "you");
        var condition = permManager.createPlatformCondition2(PlatformPerm.GLOBAL_MAP_ADMIN);

        description = "Get info about your map slots";

        setDefaultExecutor(playerOnly(this::execute));
        addSyntax(condition, playerOnly(this::execute), targetArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        MapPlayerData playerData;
        if (!context.has(targetArg)) {
            // No target specified, use self
            playerData = MapPlayerData.fromPlayer(player);
        } else {
            // Execute for the target, if they exist.
            try {
                var target = context.get(targetArg);
                var onlinePlayer = CONNECTION_MANAGER.getPlayer(target);
                if (onlinePlayer != null)
                    playerData = MapPlayerData.fromPlayer(onlinePlayer);
                else playerData = mapService.getMapPlayerData(target);
            } catch (MapService.NotFoundError ignored) {
                player.sendMessage("Player not found."); //todo
                return;
            }
        }

        showMapList(player, playerData);
    }

    private void showMapList(@NotNull Player player, @NotNull MapPlayerData playerData) {
        for (var slot = 0; slot < PlayerDataV2.MAX_MAP_SLOTS; slot++) {
            var slotId = slot + 1;
            switch (playerData.getSlotState(slot)) {
                case FILLED -> {
                    var map = mapService.getMap(playerData.id(), Objects.requireNonNull(playerData.getMapSlot(slot)));
                    var message = Component.text("Slot " + slotId + " is '")
                            .append(Component.text(map.name())
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                                    .clickEvent(ClickEvent.copyToClipboard(map.id())))
                            .append(Component.text("' "))
                            .append(Component.text("(edit)")
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to edit")))
                                    .clickEvent(ClickEvent.runCommand("/map edit " + map.id())));
                    player.sendMessage(message);
                }
                case EMPTY -> player.sendMessage("Slot " + slotId + " is empty.");
                case LOCKED -> player.sendMessage("Slot " + slotId + " is locked.");
            }
        }
    }

}
