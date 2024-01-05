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
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class StackCommand extends Command {
    private final Argument<Integer> countArg = Argument.Int("count")
            .min(1).defaultValue(1);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public StackCommand() {
        super("stack");

        addSyntax(playerOnly(this::handleStackSelection), countArg, selectionArg);
    }

    public void handleStackSelection(@NotNull Player player, @NotNull CommandContext context) {
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
        var direction = new Vec(dir.normalX(), dir.normalY(), dir.normalZ())
                // Multiply by the region size to get the offset in the direction
                // Works because direction will be (0 1 0), so only the height is affected
                .mul(region.max().sub(region.min()));

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("stack")
                .metadata() //todo
                .compute((task, world) -> {
                    //todo we can compute the known block buffer size here (which is more efficient)
                    var buffer = BlockBuffer.builder(world);
                    for (int i = 0; i < count; i++) {
                        var blockOffset = direction.mul(i + 1);

                        // Copy every block to offset
                        for (var pos : region) {
                            //todo block entities
                            buffer.set(pos.add(blockOffset), world.getBlock(pos, Block.Getter.Condition.TYPE).stateId());
                        }
                    }
                    return buffer.build();
                })
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.selection.stack",
                            Component.translatable(String.valueOf(result.blocksChanged()))));
                })
                .submit();
    }
}
