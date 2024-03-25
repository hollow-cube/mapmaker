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

public class ThruCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.thru.no_space");
    private static final double MAX_START = 100;
    private static final double MAX_WALL_THICKNESS = 10;

    public ThruCommand() {
        super("thru");

        category = CommandCategories.UTILITY;
        description = "Pass through the wall in front of you";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleThru));
    }

    private void handleThru(@NotNull Player player, @NotNull CommandContext context) {
        // Immediately find the first block in front of the player
        var position = PlayerUtil.getTargetBlock(player, MAX_START);
        if (position == null) {
            player.sendMessage(ERR_NO_SPACE);
            return;
        }

        // Walk forward on the line until the player can fit or we hit MAX_WALL_THICKNESS
        int i = 0;
        position = position.add(0.5);
        var direction = Vec.fromPoint(position.sub(player.getPosition())).normalize();
        while (!PlayerUtil.canFit(player, position)) {
            position = position.add(direction);

            if (i++ >= MAX_WALL_THICKNESS) {
                player.sendMessage(ERR_NO_SPACE);
                return;
            }
        }

        player.teleport(player.getPosition().withCoord(position));
        player.sendMessage(Component.translatable("command.thru.success"));
    }
}
