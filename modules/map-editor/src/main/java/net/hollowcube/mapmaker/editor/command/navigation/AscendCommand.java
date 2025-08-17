package net.hollowcube.mapmaker.editor.command.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

@SuppressWarnings("UnstableApiUsage")
public class AscendCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.ascend.no_space_above");

    private final Argument<Integer> levelsArg = Argument.Int("levels")
            .clamp(1, 100).defaultValue(1)
            .description("The number of levels to ascend");

    public AscendCommand() {
        super("ascend", "asc");

        category = CommandCategories.UTILITY;
        description = "Ascend to the next floor";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleAscend));
        addSyntax(playerOnly(this::handleAscend), levelsArg);
    }

    private void handleAscend(Player player, CommandContext context) {
        int levels = context.get(levelsArg);

        var instance = player.getInstance();
        var position = player.getPosition().withY(Math::floor);
        for (int i = 0; i < levels; i++) {
            // Step up until we find a solid block
            while (!(instance.getBlock(position = position.add(0, 1, 0), Block.Getter.Condition.TYPE).isSolid())) {
                if (position.blockY() > instance.getCachedDimensionType().maxY()) {
                    player.sendMessage(ERR_NO_SPACE);
                    return;
                }
            }

            // Step up until we can fit
            while (!PlayerUtil.canFit(player, position = position.add(0, 1, 0))) {
                if (position.blockY() > instance.getCachedDimensionType().maxY()) {
                    player.sendMessage(ERR_NO_SPACE);
                    return;
                }
            }
        }

        player.teleport(position);
        player.sendMessage(Component.translatable("command.ascend.success"));
    }
}
