package net.hollowcube.mapmaker.map.command.utility.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class DescendCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.descend.no_space_above");

    private final Argument<Integer> levelsArg = Argument.Int("levels")
            .clamp(1, 100).defaultValue(1)
            .description("The number of levels to descend");

    public DescendCommand() {
        super("descend", "desc");

        category = CommandCategories.UTILITY;
        description = "Descend to the floor below";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleDescend));
        addSyntax(playerOnly(this::handleDescend), levelsArg);
    }

    private void handleDescend(@NotNull Player player, @NotNull CommandContext context) {
        int levels = context.get(levelsArg);

        var instance = player.getInstance();
        var position = player.getPosition().withY(Math::floor);
        for (int i = 0; i < levels; i++) {
            // On every iteration besides the first we are starting on top of a solid block so we need to walk through the floor to find the next opening
            if (i > 0) {
                while (instance.getBlock(position = position.sub(0, 1, 0), Block.Getter.Condition.TYPE).isSolid()) {
                    if (position.blockY() < instance.getCachedDimensionType().minY()) {
                        player.sendMessage(ERR_NO_SPACE);
                        return;
                    }
                }
            }

            // Step down until we find a solid block
            while (!(instance.getBlock(position = position.sub(0, 1, 0), Block.Getter.Condition.TYPE).isSolid())) {
                if (position.blockY() < instance.getCachedDimensionType().minY()) {
                    player.sendMessage(ERR_NO_SPACE);
                    return;
                }
            }

            // Move up one block to avoid clipping into the floor
            position = position.add(0, 1, 0);

            // Step down while we can still fit
            while (PlayerUtil.canFit(player, position.sub(0, 1, 0))) {
                position = position.sub(0, 1, 0);
                if (position.blockY() < instance.getCachedDimensionType().minY()) {
                    player.sendMessage(ERR_NO_SPACE);
                    return;
                }
            }
        }

        player.teleport(position);
        player.sendMessage(Component.translatable("command.descend.success"));
    }
}
