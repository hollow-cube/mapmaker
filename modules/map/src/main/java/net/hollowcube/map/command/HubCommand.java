package net.hollowcube.map.command;

import net.hollowcube.map.event.PlayerInstanceLeaveEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.InstanceManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
        var hubInstance = MinecraftServer.getInstanceManager().getInstance(new UUID(0, 0));
        player.setInstance(hubInstance, new Pos(0, 40, 0));
        MinecraftServer.getInstanceManager().unregisterInstance(oldInstance);
//        EventDispatcher.call(new PlayerInstanceLeaveEvent(player));
    }
}
