package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.hub.feature.misc.DoubleJumpFeature;
import net.hollowcube.mapmaker.player.Permission;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.perm;

public class HubFlyCommand extends CommandDsl {

    public HubFlyCommand() {
        super("fly");

        category = CommandCategories.STAFF;
        description = "Toggles your flight on or off";

        setCondition(perm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(Player player, CommandContext context) {
        var newValue = !player.getTag(DoubleJumpFeature.TAG);
        player.sendMessage(Component.translatable("command.fly.hub", Component.translatable(newValue ? "off" : "on")));
        player.setTag(DoubleJumpFeature.TAG, newValue);
        player.setFlyingSpeed(newValue ? 0f : 0.05f);
        if (newValue) player.setFlying(false);
    }
}
