package net.hollowcube.terraform.command.region;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
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
public class SmearCommand extends CommandDsl {
    private final Argument<Integer> countArg = Argument.Int("count").min(1).defaultValue(1);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public SmearCommand() {
        super("smear");

        addSyntax(playerOnly(this::handleStackSelection));
        addSyntax(playerOnly(this::handleStackSelection), selectionArg);
        addSyntax(playerOnly(this::handleStackSelection), countArg);
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
        var direction = new Vec(dir.normalX(), dir.normalY(), dir.normalZ());

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("smear")
                .metadata() //todo
                .compute((task, world) -> {
                    //todo we can compute the known block buffer size here (which is more efficient)
                    var buffer = BlockBuffer.builder(world);
                    for (var pos : region) {
                        //todo block entities
                        var block = world.getBlock(pos, Block.Getter.Condition.TYPE);
                        if (block.isAir()) continue;

                        for (int i = 0; i < count; i++) {
                            var blockOffset = pos.add(direction.mul(i + 1));
                            var otherBlock = world.getBlock(blockOffset, Block.Getter.Condition.TYPE);
                            // If we reach a non-air block, stop smearing
                            if (!otherBlock.isAir()) break;

                            buffer.set(blockOffset, block.stateId());
                        }
                    }
                    return buffer.build();
                })
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.selection.smear",
                            Component.translatable(String.valueOf(result.blocksChanged()))));
                })
                .submit();
    }
}
