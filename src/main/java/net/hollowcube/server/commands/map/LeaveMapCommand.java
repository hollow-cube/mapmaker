package net.hollowcube.server.commands.map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import omega.mapmaker.MapMaker;

public class LeaveMapCommand extends Command {
    public LeaveMapCommand() {
        super("leave");

        setDefaultExecutor((sender, context) -> {
            Player player = (Player) sender;
            Instance baseInstance = MapMaker.getInstance().getWorldInstanceManager().getBaseInstance();
            player.setInstance(baseInstance);
            player.sendMessage(
                    Component.text("Leaving back to the real world", NamedTextColor.AQUA));
        });
    }
}
