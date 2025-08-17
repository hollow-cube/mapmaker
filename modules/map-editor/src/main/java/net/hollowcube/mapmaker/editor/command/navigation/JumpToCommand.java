package net.hollowcube.mapmaker.editor.command.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class JumpToCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.jumpto.no_space");
    private static final double MAX_DISTANCE = 100;

    public JumpToCommand() {
        super("jumpto", "j");

        category = CommandCategories.UTILITY;
        description = "Jump to the block you are looking at";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleJumpToTarget));
    }

    private void handleJumpToTarget(Player player, CommandContext context) {
        var position = PlayerUtil.getTargetBlock(player, MAX_DISTANCE, true);
        if (position == null) {
            player.sendMessage(ERR_NO_SPACE);
            return;
        }

        // Walk back on the line until we can fit
        int i = 0; // Max the number of tests at MAX_DISTANCE as a sanity check
        position = position.add(0.5, 1, 0.5);
        var direction = player.getPosition().sub(position).asVec().normalize();
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
