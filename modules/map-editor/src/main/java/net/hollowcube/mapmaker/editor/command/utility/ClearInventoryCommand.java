package net.hollowcube.mapmaker.editor.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.translatable;

public class ClearInventoryCommand extends CommandDsl {

    public ClearInventoryCommand() {
        super("clear", "clearinventory", "clearinv");

        description = "Clears all items in your inventory";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleClearInventory));
    }

    private void handleClearInventory(Player player, CommandContext context) {
        player.getInventory().clear();
        player.sendMessage(translatable("command.clear_inventory.success"));
    }

}
