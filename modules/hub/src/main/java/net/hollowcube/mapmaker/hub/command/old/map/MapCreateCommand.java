package net.hollowcube.mapmaker.hub.command.old.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SlotState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class MapCreateCommand extends BaseHubCommand {
    private static final System.Logger logger = System.getLogger(MapCreateCommand.class.getName());

    private final Argument<Integer> slotArg = ExtraArguments.MapSlot("slot", true);

    private final MapService mapService;

    public MapCreateCommand(@NotNull MapService mapService) {
        super("create");
        this.mapService = mapService;

        addSyntax(wrap(this::createMapInFirstSlot));
        addSyntax(wrap(this::createMapInSlot), slotArg);
    }

    private @Blocking void createMapInFirstSlot(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = MapPlayerData.fromPlayer(player);

        // Determine first available slot
        int slot = -1;
        for (var i = 0; i < PlayerDataV2.MAX_MAP_SLOTS; i++) {
            if (playerData.getSlotState(i) == SlotState.EMPTY) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            player.sendMessage(HubMessages.COMMAND_MAP_CREATE_NO_SLOTS_AVAILABLE);
            return;
        }

        // Create the map
        perform(player, playerData, slot);
    }

    private @Blocking void createMapInSlot(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = MapPlayerData.fromPlayer(player);

        // Argument parser will fail if the slot is not actually available, so safe to ignore checks
        //todo make sure above is actually the case.
        perform(player, playerData, context.get(slotArg));
    }

    private void perform(@NotNull Player player, @NotNull MapPlayerData playerData, int slot) {
        var resp = mapService.createMap(playerData, slot);
        switch (resp.errorCode()) {
            case null -> {
                player.sendMessage(HubMessages.COMMAND_MAP_CREATE_SUCCESS.with(resp.payload().id(), slot));
            }
            //todo handle known error cases
            default -> resp.logError(player);
        }
    }

    //todo other args to be added here
}
