package net.hollowcube.terraform.compat.vanilla.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.util.Messages;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class FillCommand extends CommandDsl {

    private final Argument<Point> fromArg = Argument.RelativeVec3("from");
    private final Argument<Point> toArg = Argument.RelativeVec3("to");
    private final Argument<Block> blockArg = Argument.Block("block");
    private final Argument<Mode> modeArg = Argument.Enum("mode", Mode.class);
    private final Argument<String> keepLiteral = Argument.Literal("keep");
    private final Argument<Block> replaceArg = Argument.Block("replace");

    public FillCommand() {
        super("fill");

        addSyntax(playerOnly(this::execute), fromArg, toArg, blockArg);
        addSyntax(playerOnly(this::execute), fromArg, toArg, blockArg, keepLiteral);
        addSyntax(playerOnly(this::execute), fromArg, toArg, blockArg, modeArg);
        Argument<String> replaceLiteral = Argument.Literal("replace");
        addSyntax(playerOnly(this::execute), fromArg, toArg, blockArg, replaceLiteral, replaceArg, modeArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var from = context.get(fromArg);
        var to = context.get(toArg);
        var block = context.get(blockArg);
        var mode = context.get(modeArg);
        var replace = context.get(replaceArg);
        var keep = context.get(keepLiteral) != null;

        Mask mask;
        if (replace != null) {
            mask = (_, _, worldBlock) -> replace.compare(worldBlock);
        } else {
            mask = keep ? Mask.air(): Mask.always();
        }

        final CuboidRegion region = new CuboidRegion(CoordinateUtil.min(from, to), CoordinateUtil.max(from, to).add(1,1,1));
        ComputeFunc func = switch (mode) {
            case OUTLINE -> RegionFunctions.outline(region, Pattern.block(block), mask);
            case HOLLOW -> RegionFunctions.cuboid(region, Pattern.block(block), true, mask);
            case null, default -> RegionFunctions.cuboid(region, Pattern.block(block), false, mask);
        };


        var session = LocalSession.forPlayer(player);
        session.buildTask("vanilla-fill")
                .metadata()
                .compute(func)
                .ephemeral()
                .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                .submit();

    }

    private enum Mode {
        DESTROY, // default
        HOLLOW,
        OUTLINE,
        STRICT // also default, (doesnt propagate block updates)
    }
}
