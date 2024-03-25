package net.hollowcube.mapmaker.map.command.utility.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class JumpToCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.jumpto.no_space");
    private static final double MAX_DISTANCE = 100;

    public JumpToCommand() {
        super("jumpto", "j");

        category = CommandCategories.UTILITY;
        description = "Jump to the block you are looking at";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleJumpToTarget));
    }

    private void handleJumpToTarget(@NotNull Player player, @NotNull CommandContext context) {
        var position = PlayerUtil.getTargetBlock(player, MAX_DISTANCE);
        if (position == null) {
            player.sendMessage(ERR_NO_SPACE);
            return;
        }

        // Walk back on the line until we can fit
        int i = 0; // Max the number of tests at MAX_DISTANCE as a sanity check
        position = position.add(0.5, 1, 0.5);
        var direction = Vec.fromPoint(player.getPosition().sub(position)).normalize();
        while (!PlayerUtil.canFit(player, position) && i++ < MAX_DISTANCE) {
            position = position.add(direction);

            // Stop if we reached the player
            if (position.distance(player.getPosition()) < 1) {
                player.sendMessage(ERR_NO_SPACE);
                return;
            }
        }

        player.teleport(player.getPosition().withCoord(position));
        player.sendMessage(Component.translatable("command.jumpto.success"));
    }
}
