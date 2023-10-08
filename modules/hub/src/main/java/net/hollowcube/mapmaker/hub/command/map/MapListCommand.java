package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
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
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapListCommand extends BaseHubCommand {
    private static final @NotNull ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final MapService mapService;
    private final Argument<String> targetArg;

    public MapListCommand(
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        super("list");
        this.mapService = mapService;

        this.targetArg = ExtraArguments.PlayerNameWithCompletion(playerService, "target");
        var condition = permManager.createPlatformCondition(PlatformPerm.GLOBAL_MAP_ADMIN);

        setDefaultExecutor(wrap(this::listMapsSelf));
        addConditionalSyntax(condition, wrap(this::listMapsOther), targetArg);
    }

    private void listMapsSelf(@NotNull Player player, @NotNull CommandContext context) {
        showMapList(player, MapPlayerData.fromPlayer(player));
    }

    private void listMapsOther(@NotNull Player player, @NotNull CommandContext context) {
        MapPlayerData playerData;

        try {
            var playerName = context.get(targetArg).trim();
            if (playerName.isEmpty()) {
                player.sendMessage("Player not found."); //todo
                return;
            }

            var onlinePlayer = CONNECTION_MANAGER.getPlayer(playerName);
            if (onlinePlayer != null)
                playerData = MapPlayerData.fromPlayer(onlinePlayer);
            else playerData = mapService.getMapPlayerData(playerName);
        } catch (MapService.NotFoundError e) {
            player.sendMessage("Player not found."); //todo
            return;
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
