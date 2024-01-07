package net.hollowcube.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class FlyCommand extends CommandDsl {
    public FlyCommand() {
        super("fly");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(@NotNull Player player, @NotNull CommandContext context) {
        if (player.isAllowFlying()) disableFly(player);
        else enableFly(player);
    }

    private void enableFly(@NotNull Player target) {
        target.setAllowFlying(true);
        target.setFlying(true);
        target.sendMessage(MapMessages.COMMAND_FLY_ENABLED);
    }

    private void disableFly(@NotNull Player target) {
        target.setAllowFlying(false);
        target.setFlying(false);
        target.sendMessage(MapMessages.COMMAND_FLY_DISABLED);
    }
}
