package net.hollowcube.terraform.command.old;

import net.hollowcube.terraform.command.old.helper.TerraformCommand;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolCommand extends TerraformCommand {
    private final ToolHandler toolHandler;

    public ToolCommand(@Nullable CommandCondition condition, @NotNull ToolHandler toolHandler) {
        super("tool", "tf:tool");
        setCondition(condition);
        this.toolHandler = toolHandler;

        addSubcommand(new Create());
    }

    class Create extends Command {
        private final Argument<String> builtinToolArg = ArgumentType.Word("type")
                .from(toolHandler.getToolNames().toArray(new String[0]));

        public Create() {
            super("create");

            setDefaultExecutor(wrap(this::createEmptyTool));
            addSyntax(wrap(this::createBuiltinTool), builtinToolArg);
        }

        private void createEmptyTool(@NotNull Player player, @NotNull CommandContext context) {
            player.sendMessage("custom tools not supported yet :(");
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
}
