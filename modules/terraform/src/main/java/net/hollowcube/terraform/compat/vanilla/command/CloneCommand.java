package net.hollowcube.terraform.compat.vanilla.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.mask.BlockMask;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CloneCommand extends CommandDsl {

    private final Argument<Point> fromArg = Argument.RelativeVec3("begin");
    private final Argument<Point> toArg = Argument.RelativeVec3("end");
    private final Argument<Point> destinationArg = Argument.RelativeVec3("destination");
    private final Argument<Block> filterArg = Argument.Block("filter");
    private final Argument<Mode> modeArg = Argument.Enum("mode", Mode.class).defaultValue(Mode.NORMAL);
    private final Argument<CopyMode> copyModeArg = Argument.Enum("copy_mode", CopyMode.class).defaultValue(CopyMode.REPLACE);

    private final List<List<Argument<?>>> defaults = new ArrayList<>();
    private final Argument<String> strictLiteral = Argument.Literal("strict"); // doesn't change anything because we don't have block updates either way

    public CloneCommand() {
        super("clone");

        // to and from are here for vanilla parity, filtered is detected via the filterArg
        Argument<String> fromLiteral = Argument.Literal("from");
        Argument<String> fromArgId = Argument.Word("from_id");
        Argument<String> toLiteral = Argument.Literal("to");
        Argument<String> toArgId = Argument.Word("to_id");
        Argument<String> filteredLiteral = Argument.Literal("filtered");

        defaults.add(List.of(fromArg, toArg, destinationArg));
        defaults.add(List.of(fromLiteral, fromArgId, fromArg, toArg, destinationArg));
        defaults.add(List.of(fromLiteral, fromArgId, fromArg, toArg, toLiteral, toArgId, destinationArg));

        addWithStrict();
        addWithStrict(copyModeArg);
        addWithStrict(copyModeArg, modeArg);
        addWithStrict(filteredLiteral, filterArg);
        addWithStrict(filteredLiteral, filterArg, modeArg);
    }

    private void addWithStrict(@NotNull Argument<?>... args) {
        var argList = new ArrayList<>(Arrays.asList(args));
        argList.addFirst(strictLiteral);
        addWithBase(argList.toArray(Argument[]::new));
        addWithBase(args);
    }

    private void addWithBase(@NotNull Argument<?>... args) {
        for (List<Argument<?>> aDefault : defaults) {
            addSyntax(playerOnly(this::execute), Stream.of(aDefault, Arrays.asList(args))
                    .flatMap(List::stream).toArray(Argument[]::new));
        }
     }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var start = context.get(fromArg);
        var end = context.get(toArg);
        var destination = context.get(destinationArg);
        var filter = context.get(filterArg);
        var mode = context.get(modeArg);
        var copyMode = context.get(copyModeArg);

        var sourceMask = Mask.always();
        if (copyMode == CopyMode.MASKED) {
            sourceMask = Mask.not(Mask.air());
        }
        if (filter != null) {
            sourceMask = Mask.and(sourceMask, new BlockMask(filter.id(), filter.properties()));
        }
        if (CoordinateUtil.isBetween(CoordinateUtil.min(start, end), CoordinateUtil.max(start, end), destination) && mode != Mode.FORCE) {
            player.sendMessage(Component.translatable("terraform.command.vanilla.clone.intersets"));
            return;
        }
        Point min = CoordinateUtil.min(start, end), max = CoordinateUtil.max(start, end).add(1, 1, 1);


        var session = LocalSession.forPlayer(player);
        session.buildTask("vanilla-clone")
                .metadata() // todo
                .compute(RegionFunctions.clone(min, max, destination, sourceMask, mode == Mode.MOVE))
                .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                .submit();
    }

    private enum CopyMode {
        MASKED, // copy only non-air block, still replace all blocks
        REPLACE, // copy all blocks and replace whole destination area
    }

    private enum Mode {
        FORCE, // ignore overlap of source and destination
        MOVE, // replace copied blocks with air
        NORMAL, // default
    }
}
