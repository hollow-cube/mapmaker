package net.hollowcube.terraform.command.tool;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ToolCreateCommand extends Command {
    private final Argument<String> builtinToolArg;

    private final ToolHandler toolHandler;

    public ToolCreateCommand(@NotNull ToolHandler toolHandler) {
        super("create");
        this.toolHandler = toolHandler;

        builtinToolArg = Argument.Word("type").with(toolHandler.getToolNames());

        addSyntax(playerOnly(this::createBuiltinTool), builtinToolArg);
    }

    private void createBuiltinTool(@NotNull Player player, @NotNull CommandContext context) {
        var toolType = context.get(builtinToolArg);
        var toolItem = toolHandler.createBuiltinTool(toolType);
        smartAddToolItem(player, toolItem);
        player.sendMessage("created tool blah blah");
    }

    private void smartAddToolItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        // If their current item is empty, just set it
        var current = player.getInventory().getItemInMainHand();
        if (current.isAir()) {
            player.getInventory().setItemInMainHand(itemStack);
            return;
        }

        // Otherwise, try another hotbar slot
        for (int slot = 0; slot < 9; slot++) {
            var slotItem = player.getInventory().getItemStack(slot);
            if (slotItem.isAir()) {
                player.getInventory().setItemStack(slot, itemStack);
                return;
            }
        }

        // Otherwise, try to add normally to inventory
        var added = player.getInventory().addItemStack(itemStack, TransactionOption.ALL_OR_NOTHING);
        if (added) return;

        // Finally, just replace their main hand item
        player.getInventory().setItemInMainHand(itemStack);
    }

}
