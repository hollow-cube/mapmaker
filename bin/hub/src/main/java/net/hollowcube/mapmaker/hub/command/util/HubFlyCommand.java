package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.hub.feature.misc.DoubleJumpFeature;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubFlyCommand extends CommandDsl {

    public HubFlyCommand(@NotNull PermManager permManager) {
        super("fly");

        category = CommandCategories.STAFF;
        description = "Toggles your flight on or off";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(@NotNull Player player, @NotNull CommandContext context) {
        var newValue = !player.getTag(DoubleJumpFeature.TAG);
        player.sendMessage(Component.translatable("command.fly.hub", Component.translatable(newValue ? "off" : "on")));
        player.setTag(DoubleJumpFeature.TAG, newValue);
        player.setFlyingSpeed(newValue ? 0f : 0.05f);
        if (newValue) player.setFlying(false);
    }
}
