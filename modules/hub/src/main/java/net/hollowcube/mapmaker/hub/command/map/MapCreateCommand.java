package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class MapCreateCommand extends BaseHubCommand {
    private static final System.Logger logger = System.getLogger(MapCreateCommand.class.getName());

    private final Argument<Integer> slotArg = ExtraArguments.MapSlot("slot", true);

    private final MapService mapService;
    private final HubHandler handler;

    public MapCreateCommand(@NotNull MapService mapService, @NotNull HubHandler handler) {
        super("create");
        this.mapService = mapService;
        this.handler = handler;

        addSyntax(wrap(this::createMapInFirstSlot));
        addSyntax(wrap(this::createMapInSlot), slotArg);
    }

    private @Blocking void createMapInFirstSlot(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerData.fromPlayer(player);

        // Determine first available slot
        int slot = -1;
        for (var i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
            if (playerData.getSlotState(i) == PlayerData.SLOT_STATE_OPEN) {
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
        var playerData = PlayerData.fromPlayer(player);

        // Argument parser will fail if the slot is not actually available, so safe to ignore checks
        //todo make sure above is actually the case.
        perform(player, playerData, context.get(slotArg));
    }

    private void perform(@NotNull Player player, @NotNull PlayerData playerData, int slot) {
        try {
            var mapData = handler.createMapForPlayerInSlot(playerData, slot);
            player.sendMessage(HubMessages.COMMAND_MAP_CREATE_SUCCESS.with(mapData.id(), slot));
        } catch (Exception e) {
            //todo handle known exception cases
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(GenericMessages.COMMAND_UNKNOWN_ERROR);
        }
    }

    //todo other args to be added here
}
