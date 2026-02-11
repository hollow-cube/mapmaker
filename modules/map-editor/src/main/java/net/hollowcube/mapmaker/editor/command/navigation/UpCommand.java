package net.hollowcube.mapmaker.editor.command.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
//import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class UpCommand extends CommandDsl {
//    private static final Component ERR_NO_SPACE = Component.translatable("command.up.no_space");

    private final Argument<Integer> distanceArg = Argument.Int("distance").clamp(-500, 500).defaultValue(1)
            .description("The number of blocks to move");

    public UpCommand() {
        super("up");

        category = CommandCategories.UTILITY;
        description = "Move vertically and place a block at the target location";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleJumpToTarget));
        addSyntax(playerOnly(this::handleJumpToTarget), distanceArg);
    }

    private void handleJumpToTarget(Player player, CommandContext context) {
        var instance = player.getInstance();
        int minY = instance.getCachedDimensionType().minY() + 1;
        int maxY = instance.getCachedDimensionType().maxY();

        var target = player.getPosition().withY(Math.clamp(player.getPosition().y() + context.get(distanceArg), minY, maxY));

        // Ensure they can actually get from the current position to the target without hitting anything
        // commenting this out but not deleting it in case someone wants to make it so when it goes through this logic it warns you that you'll be placed in a block and then says run this command again to confirm
//        if (!PlayerUtil.canMoveTo(player, target)) {
//            player.sendMessage(ERR_NO_SPACE);
//            return;
//        }

        if (instance.getBlock(target.sub(0, 1, 0)).isAir()) {
            instance.setBlock(target.sub(0, 1, 0), Block.GLASS);
        }

        player.teleport(target);
        player.sendMessage(Component.translatable("command.jumpto.success", Component.text(target.blockY())));
    }
}
