package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.mask.OffsetMask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.selection.region.CuboidRegionSelector;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.util.Messages;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;

public final class RegionCommands {
    private RegionCommands() {
    }

    public static final class Set extends WECommand {
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Set() {
            super("/set");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var generator = RegionFunctions.replace(region, Mask.always(), pattern);

            session.buildTask("we-set")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Line extends WECommand {
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");
        // todo thickness, -h (hollow)

        public Line() {
            super("/line");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            final Pattern pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            if (selection.region() == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            if (selection.selector() instanceof CuboidRegionSelector regionSelector) { // todo add line selection once selection switching is added
                var generator = RegionFunctions.line(regionSelector.getPos1(), regionSelector.getPos2(), pattern);

                session.buildTask("we-line")
                        .metadata() //todo
                        .compute(generator)
                        .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                        .submit();
                return;
            }

            player.sendMessage(Messages.REGION_NOT_CUBOID);
        }
    }

    public static class Curve extends WECommand {
        public Curve() {
            super("/curve");
        }
    }

    public static class Replace extends WECommand {
        private final Argument<Mask> maskArg = WEArgument.Mask("mask").defaultValue(Mask.not(Mask.air()));
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Replace() {
            super("/replace");

            addSyntax(playerOnly(this::execute), maskArg);
            addSyntax(playerOnly(this::execute), maskArg, patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var mask = context.get(maskArg);
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var generator = RegionFunctions.replace(region, mask, pattern);

            session.buildTask("we-replace")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Overlay extends WECommand {
        // Masks any air block above a non-air block
        private static final Mask OVERLAY_MASK = Mask.and(Mask.air(), OffsetMask.overlay(Mask.not(Mask.air())));

        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Overlay() {
            super("/overlay");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var generator = RegionFunctions.replace(region, OVERLAY_MASK, pattern);

            session.buildTask("we-overlay")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Center extends WECommand {
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Center() {
            super("/center", "/middle");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            // This code is braindead and I(matt) DO NOT CARE. I was very tired and spent a ludicrous amount of
            // time writing this given the fact that it is 100% stupid. I hate it.
            var size = region.max().sub(region.min());
            var centerMin = region.min().add(region.max()).div(2);
            var centerMax = centerMin.add(1);
            centerMin = centerMin.sub(
                    size.blockX() % 2 == 0 ? 1 : 0,
                    size.blockY() % 2 == 0 ? 1 : 0,
                    size.blockZ() % 2 == 0 ? 1 : 0
            );

            session.buildTask("we-center")
                    .metadata() //todo
                    .compute(ComputeFunc.set(new CuboidRegion(centerMin, centerMax), pattern))
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Naturalize extends WECommand {
        public Naturalize() {
            super("/naturalize");
        }
    }

    public static class Walls extends WECommand {
        private static final Direction[] FACES = Direction.HORIZONTAL;

        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Walls() {
            super("/walls");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }
            if (!(region instanceof CuboidRegion cuboid)) {
                player.sendMessage(Messages.REGION_NOT_CUBOID);
                return;
            }

            var generator = RegionFunctions.cuboid(cuboid, pattern, true, FACES);

            session.buildTask("we-walls")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }

    }

    public static class Faces extends WECommand {
        private static final Direction[] FACES = Direction.values();

        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");

        public Faces() {
            super("/faces");

            addSyntax(playerOnly(this::execute), patternArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }
            if (!(region instanceof CuboidRegion cuboid)) {
                player.sendMessage(Messages.REGION_NOT_CUBOID);
                return;
            }

            var generator = RegionFunctions.cuboid(cuboid, pattern, true, FACES);

            session.buildTask("we-faces")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Smooth extends WECommand {
        public Smooth() {
            super("/smooth");
        }
    }

    public static class Move extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Integer> countArg = Argument.Int("count").min(1).defaultValue(1);
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");
        private final Argument<Pattern> replaceArg = WEArgument.Pattern("replace").defaultValue(Pattern.air());
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");

        private enum Flags {
            SHIFT_SELECTION,
            AIR_SKIP,
            ENTITIES,
            BIOMES,
            MASK,
        }

        public Move() {
            super("/move");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), countArg);
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), countArg, directionArg);
            addSyntax(playerOnly(this::execute), countArg, directionArg, replaceArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg, directionArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg, directionArg, replaceArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg, directionArg, replaceArg, maskArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var count = context.get(countArg);
            var direction = context.get(directionArg);
            var replace = context.get(replaceArg);
            var mask = context.get(maskArg);

            if (flags.contains(Flags.MASK)) {
                Check.notNull(mask, "mask is required with the mask flag");
            } else {
                mask = Mask.always();
            }

            // Warnings for currently unsupported flags
            if (flags.contains(Flags.ENTITIES))
                player.sendMessage(Messages.GENERIC_ENTITIES_UNSUPPORTED);
            if (flags.contains(Flags.BIOMES))
                player.sendMessage(Messages.GENERIC_BIOMES_UNSUPPORTED);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            if (flags.contains(Flags.AIR_SKIP)) {
                mask = Mask.and(mask, Mask.not(Mask.air()));
            }

            var generator = RegionFunctions.move(region, direction, count, replace, mask);

            session.buildTask("we-move")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> {
                        if (flags.contains(Flags.SHIFT_SELECTION)) {
                            try {
                                var normal = new Vec(direction.normalX(), direction.normalY(), direction.normalZ());
                                selection.reshape(normal.mul(count), normal.mul(count));
                            } catch (UnsupportedOperationException e) {
                                player.sendMessage(Messages.SELECTION_RESHAPE_UNSUPPORTED);
                            }
                        }
                        player.sendMessage(Messages.SELECTION_MOVED.with(result.blocksChanged()));
                    })
                    .submit();
        }
    }

    public static class Stack extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Integer> countArg = Argument.Int("count").min(1).defaultValue(1);
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");

        private enum Flags {
            SHIFT_SELECTION,
            AIR_SKIP,
            ENTITIES,
            BIOMES,
            MASK,
        }

        public Stack() {
            super("/stack");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), countArg);
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), countArg, directionArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg, directionArg);
            addSyntax(playerOnly(this::execute), flagsArg, countArg, directionArg, maskArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var count = context.get(countArg);
            var direction = context.get(directionArg);
            var mask = context.get(maskArg);

            if (flags.contains(Flags.MASK)) {
                Check.notNull(mask, "mask is required with the mask flag");
            } else {
                mask = Mask.always();
            }

            // Warnings for currently unsupported flags
            if (flags.contains(Flags.ENTITIES))
                player.sendMessage(Messages.GENERIC_ENTITIES_UNSUPPORTED);
            if (flags.contains(Flags.BIOMES))
                player.sendMessage(Messages.GENERIC_BIOMES_UNSUPPORTED);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            if (flags.contains(Flags.AIR_SKIP)) {
                mask = Mask.and(mask, Mask.not(Mask.air()));
            }

            var generator = RegionFunctions.stack(region, direction, count, mask);

            session.buildTask("we-stack")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> {
                        if (flags.contains(Flags.SHIFT_SELECTION)) {
                            try {
                                var normal = new Vec(direction.normalX(), direction.normalY(), direction.normalZ());
                                selection.reshape(normal.mul(count), normal.mul(count));
                            } catch (UnsupportedOperationException e) {
                                player.sendMessage(Messages.SELECTION_RESHAPE_UNSUPPORTED);
                            }
                        }
                        player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged()));
                    })
                    .submit();
        }
    }

    //todo this does NOT come from worldedit!!!
    public static class Smear extends WECommand {
        private final Argument<Integer> countArg = Argument.Int("count").min(1).defaultValue(1);
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");

        public Smear() {
            super("/smear");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), countArg);
            addSyntax(playerOnly(this::execute), countArg, directionArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var count = context.get(countArg);
            var direction = context.get(directionArg);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var dir = Objects.requireNonNullElseGet(direction, () -> DirectionUtil.fromView(player.getPosition()));
            var offset = new Vec(dir.normalX(), dir.normalY(), dir.normalZ());

            // Execute the change
            session.buildTask("we-smear")
                    .metadata() //todo
                    .compute((task, world) -> {
                        //todo we can compute the known block buffer size here (which is more efficient)
                        var buffer = BlockBuffer.builder(world);
                        for (var pos : region) {
                            var block = world.getBlock(pos);
                            if (block.isAir()) continue;

                            for (int i = 0; i < count; i++) {
                                var blockOffset = pos.add(offset.mul(i + 1));
                                var otherBlock = world.getBlock(blockOffset, Block.Getter.Condition.TYPE);
                                // If we reach a non-air block, stop smearing
                                if (!otherBlock.isAir()) break;

                                buffer.set(blockOffset, block);
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

    public static class Hollow extends WECommand {
        public Hollow() {
            super("/hollow");
        }
    }

}
