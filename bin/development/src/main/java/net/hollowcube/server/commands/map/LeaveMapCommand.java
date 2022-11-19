package net.hollowcube.server.commands.map;

import net.hollowcube.server.MapMaker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public class LeaveMapCommand extends Command {
    public LeaveMapCommand() {
        super("leave");

        setDefaultExecutor((sender, context) -> {
            if(sender instanceof Player player) {
                Instance baseInstance = MapMaker.getInstance().getWorldInstanceManager().getBaseInstance();
                if(baseInstance != player.getInstance()) {
                    player.setInstance(baseInstance);
                    player.sendMessage(
                            Component.text("Leaving back to the real world", NamedTextColor.AQUA));
                } else {
                    player.sendMessage(Component.text("You are already in the real world!", NamedTextColor.RED));
                }
            }

        });
    }
}
