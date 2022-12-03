package net.hollowcube.map.command;

import net.hollowcube.mapmaker.hub.HubManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubCommand extends BaseMapCommand {
    public HubCommand() {
        super("hub");

        setDefaultExecutor(this::returnToHub);
    }

    private void returnToHub(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            return;
        }

        var oldInstance = player.getInstance();

        sender.sendMessage("Returning to hub");
        HubManager.TemporaryIAmTerrible.INSTANCE.sendToHub(player);
    }
}
