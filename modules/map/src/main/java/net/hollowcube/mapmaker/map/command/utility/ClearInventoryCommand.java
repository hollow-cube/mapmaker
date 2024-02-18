package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class ClearInventoryCommand extends CommandDsl {
    public ClearInventoryCommand() {
        super("clear", "clearinventory", "clearinv");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleClearInventory));
    }

    private void handleClearInventory(@NotNull Player player, @NotNull CommandContext context) {
        player.getInventory().clear();
        player.sendMessage(MapMessages.COMMAND_CLEAR_INVENTORY_SUCCESS);
    }
}
