package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapListCommand extends BaseHubCommand {

    private final MapService mapService;

    public MapListCommand(@NotNull MapService mapService) {
        super("list");
        this.mapService = mapService;

        addSyntax(wrap(this::showMapInfo));
    }

    private void showMapInfo(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerDataV2.fromPlayer(player);
        for (var slot = 0; slot < PlayerDataV2.MAX_MAP_SLOTS; slot++) {
            var slotId = slot + 1;
            switch (playerData.getSlotState(slot)) {
                case FILLED -> {
                    var map = mapService.getMap(playerData.id(), Objects.requireNonNull(playerData.getMapSlot(slot)));
                    player.sendMessage("Slot " + slotId + " is in use with map " + map.id() + " (" + map.settings().getName() + ")");
                }
                case EMPTY -> player.sendMessage("Slot " + slotId + " is empty.");
                case LOCKED -> player.sendMessage("Slot " + slotId + " is locked.");
            }
        }
    }

}
