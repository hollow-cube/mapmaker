package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubFlyCommand extends Command {

    public HubFlyCommand(@NotNull PermManager permManager) {
        super("fly");
        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));

        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(@NotNull Player player, @NotNull CommandContext context) {
        var newValue = !player.getTag(HubServer.DOUBLE_JUMP_TAG);
        player.sendMessage("set flight to " + (newValue ? "off" : "on"));
        player.setTag(HubServer.DOUBLE_JUMP_TAG, newValue);
        player.setFlyingSpeed(newValue ? 0f : 0.05f);
    }
}
