package net.hollowcube.map.command;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand extends BaseMapCommand {

    public FlyCommand() {
            super(true, "fly", "flight");
            setDefaultExecutor((sender, context) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command!");
                } else {
                    if (player.isAllowFlying()) disableFly(player);
                    else enableFly(player);
                }
            });
        }

        public void enableFly(@NotNull Player target) {
            target.setAllowFlying(true);
            target.setFlying(true);
            target.sendMessage("Flight enabled.");
        }

        public void disableFly(@NotNull Player target) {
            target.setAllowFlying(false);
            target.setFlying(false);
            target.sendMessage("Flight disabled.");
    }
}
