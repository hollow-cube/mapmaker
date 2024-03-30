package net.hollowcube.mapmaker.map.command.utility.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class UpCommand extends CommandDsl {
    private static final Component ERR_NO_SPACE = Component.translatable("command.up.no_space");

    private final Argument<Integer> distanceArg = Argument.Int("distance").clamp(1, 500).defaultValue(1)
            .description("The number of blocks to ascend");

    public UpCommand() {
        super("up");

        category = CommandCategories.UTILITY;
        description = "Jump to the block you are looking at";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleJumpToTarget));
        addSyntax(playerOnly(this::handleJumpToTarget), distanceArg);
    }

    private void handleJumpToTarget(@NotNull Player player, @NotNull CommandContext context) {
        var instance = player.getInstance();

        var target = player.getPosition().add(0, context.get(distanceArg), 0);
        if (target.blockY() > instance.getDimensionType().getMaxY()) {
            player.sendMessage(ERR_NO_SPACE);
            return;
        }

        // Ensure they can actually get from the current position to the target without hitting anything
        if (!PlayerUtil.canMoveTo(player, target)) {
            player.sendMessage(ERR_NO_SPACE);
            player.sendMessage("hit");
            return;
        }

        instance.setBlock(target.sub(0, 1, 0), Block.GLASS);
        player.teleport(target);
        player.sendMessage(Component.translatable("command.jumpto.success"));
    }
}
