package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public final class UtilityCommands {

    private UtilityCommands() {
    }

    public static class Fill extends WECommand {
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern");
        private final Argument<Integer> radiusArg = Argument.Int("radius").min(1).max(300);
        private final Argument<Integer> depthArg = Argument.Int("depth").min(1).max(300).defaultValue(300);

        public Fill() {
            super("/fill", "/fillr");

            addSyntax(playerOnly(this::execute), patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), patternArg, radiusArg, depthArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);
            var radius = context.get(radiusArg);
            var depth = context.get(depthArg);

            var center = player.getPosition();
            @SuppressWarnings("UnstableApiUsage")
            var minWorldY = player.getInstance().getCachedDimensionType().minY();
            var minY = Math.max(center.y() - depth, minWorldY);


            var computeFunc = RegionFunctions.floodFill(center, radius, pattern, (_, point, block) -> {
                if (!block.isAir()) {
                    return false;
                }

                return point.blockY() <= center.blockY() && point.blockY() > minY;
            });

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-fill")
                    .metadata() //todo
                    .compute(computeFunc)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Drain extends WECommand {
        private final Argument<Integer> radiusArg = Argument.Int("radius").min(1).max(300);
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);

        public Drain() {
            super("/drain");

            addSyntax(playerOnly(this::execute), radiusArg);
            addSyntax(playerOnly(this::execute), radiusArg, flagsArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var radius = context.get(radiusArg);
            var flags = context.get(flagsArg);

            var center = player.getPosition();

            var computeFunc = RegionFunctions.drain(center, radius, (_, _, block) -> !flags.contains(Flags.KEEP_WATERLOGGED) || !"true".equals(block.getProperty("waterlogged")));

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-fill")
                    .metadata() //todo
                    .compute(computeFunc)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }

        enum Flags {
            KEEP_WATERLOGGED,
            WATERLOGGED // to allow copied commands from the internet to still work, it's our default behaviour to un-waterlog
        }
    }

    public static class FixLava extends WECommand {
        public FixLava() {
            super("/fixlava");
        }
    }

    public static class FixWater extends WECommand {
        public FixWater() {
            super("/fixwater");
        }
    }

    public static class RemoveAbove extends WECommand {
        private final Argument<Integer> sizeArg = Argument.Int("size").min(1).max(100).defaultValue(1);
        // The value is clamped to the world border, so 10k will ensure we hit that
        private final Argument<Integer> heightArg = Argument.Int("height").min(1).defaultValue(10_000);

        public RemoveAbove() {
            super("/removeabove");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), sizeArg);
            addSyntax(playerOnly(this::execute), sizeArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var size = context.get(sizeArg);
            var height = context.get(heightArg);

            var center = player.getPosition();
            var dimensionMax = player.getInstance().getCachedDimensionType().maxY();
            var max = Math.min(center.blockY() + height, dimensionMax);

            var region = new CuboidRegion(center.sub(size - 1).withY(center.blockY()), center.add(size).withY(max));
            var generator = RegionFunctions.replace(region, Mask.always(), Pattern.air());

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-removeabove")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class RemoveBelow extends WECommand {
        private final Argument<Integer> sizeArg = Argument.Int("size").min(1).max(100).defaultValue(1);
        // The value is clamped to the world border, so 10k will ensure we hit that
        private final Argument<Integer> heightArg = Argument.Int("height").min(1).defaultValue(10_000);

        public RemoveBelow() {
            super("/removebelow");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), sizeArg);
            addSyntax(playerOnly(this::execute), sizeArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var size = context.get(sizeArg);
            var height = context.get(heightArg);

            var center = player.getPosition();
            var dimensionMin = player.getInstance().getCachedDimensionType().minY();
            var min = Math.max(center.blockY() - height, dimensionMin);

            var region = new CuboidRegion(center.sub(size - 1).withY(min), center.add(size).withY(center.blockY()));
            var generator = RegionFunctions.replace(region, Mask.always(), Pattern.air());

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-removebelow")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class RemoveNear extends WECommand {
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");
        private final Argument<Integer> radiusArg = Argument.Int("radius").min(1).max(100).defaultValue(50);

        public RemoveNear() {
            super("/removenear");

            addSyntax(playerOnly(this::execute), maskArg);
            addSyntax(playerOnly(this::execute), maskArg, radiusArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var mask = context.get(maskArg);
            var radius = context.get(radiusArg);

            var center = player.getPosition();
            var region = new CuboidRegion(center.sub(radius - 1), center.add(radius));
            var generator = RegionFunctions.replace(region, mask, Pattern.air());

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-removenear")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class ReplaceNear extends WECommand {
        private static final Mask DEFAULT_MASK = Mask.not(Mask.air());

        private final Argument<Integer> radiusArg = Argument.Int("radius").min(1).max(100);
        private final Argument<Mask> fromArg = WEArgument.Mask("from").defaultValue(DEFAULT_MASK);
        private final Argument<Pattern> toArg = WEArgument.Pattern("to");

        public ReplaceNear() {
            super("/replacenear");

            addSyntax(playerOnly(this::execute), radiusArg, toArg);
            addSyntax(playerOnly(this::execute), radiusArg, fromArg, toArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var radius = context.get(radiusArg);
            var fromMask = context.get(fromArg);
            var toPattern = context.get(toArg);

            var center = player.getPosition();
            var region = new CuboidRegion(center.sub(radius - 1), center.add(radius));
            var generator = RegionFunctions.replace(region, fromMask, toPattern);

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-replacenear")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Help extends WECommand {
        public Help() {
            super("/help");
        }
    }
}
