package net.hollowcube.terraform.command.region;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class MoveCommand extends Command {
    private final Argument<Integer> countArg = Argument.Int("count")
            .min(1).defaultValue(1);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public MoveCommand() {
        super("move");

        addSyntax(playerOnly(this::handleMoveSelection), countArg, selectionArg);
    }

    private void handleMoveSelection(@NotNull Player player, @NotNull CommandContext context) {
        var selection = context.get(selectionArg);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        var count = context.get(countArg);
        if (count < 1 || count > 100) {
            player.sendMessage(Component.translatable("generic.number.not_in_range", Component.text(1), Component.text(100)));
            return;
        }

        var dir = CoordinateUtil.directionFromView(player.getPosition());
        var offset = new Vec(dir.normalX(), dir.normalY(), dir.normalZ())
                // Multiply by the region size to get the offset in the direction
                // Works because direction will be (0 1 0), so only the height is affected
                .mul(count);

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("move")
                .metadata() //todo
                .compute((task, world) -> {
                    //todo we can compute the known block buffer size here (which is more efficient)
                    var buffer = BlockBuffer.builder(world);
                    for (var pos : region) {
                        Block block = world.getBlock(pos);
                        // Don't move air
                        if (block.isAir()) continue;

                        buffer.set(pos, Block.AIR); // Fill block buffer with air at old locations to delete our current selection
                        // We do this first in order to not overwrite our valid blocks with air
                    }
                    for (var pos : region) {
                        Block block = world.getBlock(pos);
                        // Don't move air
                        if (block.isAir()) continue;

                        buffer.set(pos.add(offset), block); // Fill block buffer with our valid blocks at the new location
                    }
                    return buffer.build();
                })
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.selection.move",
                            Component.translatable(String.valueOf(result.blocksChanged()))));
                    // Move selection to match the move command
                    // builders don't like it this behavior. Should be an optional flag
//                    Point newPrimary = selection.region().min().add(offset);
//                    Point newSecondary = selection.region().max().add(offset);
//                    selection.selectPrimary(newPrimary, false);
//                    selection.selectSecondary(newSecondary, false);
                })
                .submit();
    }
}
