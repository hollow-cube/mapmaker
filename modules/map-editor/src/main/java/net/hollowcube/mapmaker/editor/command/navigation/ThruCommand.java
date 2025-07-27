package net.hollowcube.mapmaker.editor.command.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class ThruCommand extends CommandDsl {

    private static final Component ERR_NO_SPACE = Component.translatable("command.thru.no_space");
    private static final double MAX_START = 100;
    private static final double MAX_WALL_THICKNESS = 10;

    public ThruCommand() {
        super("thru");

        category = CommandCategories.UTILITY;
        description = "Pass through the wall in front of you";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleThru));
    }

    private void handleThru(Player player, CommandContext context) {
        // Immediately find the first block in front of the player
        var position = PlayerUtil.getTargetBlock(player, MAX_START, false);
        if (position == null) {
            player.sendMessage(ERR_NO_SPACE);
            return;
        }

        // Walk forward on the line until the player can fit or we hit MAX_WALL_THICKNESS
        int i = 0;
        position = position.add(0.5);
        var direction = position.sub(player.getPosition()).asVec().normalize();
        while (!PlayerUtil.canFit(player, position)) {
            position = position.add(direction);

            if (!player.getInstance().getWorldBorder().inBounds(position) || i++ >= MAX_WALL_THICKNESS) {
                player.sendMessage(ERR_NO_SPACE);
                return;
            }
        }

        int x = position.blockX();
        int y = position.blockY();
        int z = position.blockZ();

        player.teleport(player.getPosition().withCoord(x + 0.5, y, z + 0.5));
        player.sendMessage(Component.translatable("command.thru.success"));
    }
}
