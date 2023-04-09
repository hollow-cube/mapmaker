package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand extends BaseMapCommand {

    public FlyCommand() {
        super(true, "fly", "flight");
        setDefaultExecutor(this::handleToggle);
    }

    public void handleToggle(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        if (player.isAllowFlying()) disableFly(player);
        else enableFly(player);
    }

    public void enableFly(@NotNull Player target) {
        target.setAllowFlying(true);
        target.setFlying(true);
        target.sendMessage(MapMessages.COMMAND_FLY_ENABLED);
    }

    public void disableFly(@NotNull Player target) {
        target.setAllowFlying(false);
        target.setFlying(false);
        target.sendMessage(MapMessages.COMMAND_FLY_DISABLED);
    }
}
