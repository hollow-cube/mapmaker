package net.hollowcube.map.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearInventoryCommand extends BaseMapCommand {

    public ClearInventoryCommand() {
        super(true, "clearinventory", "clearinv", "ci");
        setDefaultExecutor(this::clearInventory);
    }

    private void clearInventory(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }
        ((Player) sender).getInventory().clear();
        sender.sendMessage("Your inventory has been cleared.");
    }
}