package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.model.PlayerData;
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
        var playerData = PlayerData.fromPlayer(player);
        for (var slot = 0; slot < PlayerData.MAX_MAP_SLOTS; slot++) {
            var slotId = slot + 1;
            switch (playerData.getSlotState(slot)) {
                case PlayerData.SLOT_STATE_IN_USE -> {
                    var map = mapService.getMap(playerData.getId(), Objects.requireNonNull(playerData.getMapSlot(slot)));
                    player.sendMessage("Slot " + slotId + " is in use with map " + map.id() + " (" + map.settings().getName() + ")");
                }
                case PlayerData.SLOT_STATE_OPEN -> player.sendMessage("Slot " + slotId + " is empty.");
                case PlayerData.SLOT_STATE_LOCKED -> player.sendMessage("Slot " + slotId + " is locked.");
            }
        }
    }

}
