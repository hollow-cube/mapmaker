package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.compute.ShapeFunctions;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public final class GenerationCommands {

    public static class HCyl extends WECommand {
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Vec> radiusArg = WEArgument.CommaSeparatedVec2("radii");
        private final Argument<Integer> heightArg = Argument.Int("height").clamp(1, 100).defaultValue(1);


        public HCyl() {
            super("/hcyl");

            addSyntax(playerOnly(this::execute), patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), patternArg, radiusArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);
            var radius = (Vec) CoordinateUtil.max(context.get(radiusArg), Vec.ONE);
            var height = context.get(heightArg);

            var generator = ShapeFunctions.cylinder(player.getPosition(), pattern, radius.withY(height), true);

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-hcyl")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Cyl extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Vec> radiusArg = WEArgument.CommaSeparatedVec2("radii");
        private final Argument<Integer> heightArg = Argument.Int("height").clamp(1, 100).defaultValue(1);

        private enum Flags {
            HOLLOW
        }

        public Cyl() {
            super("/cyl");

            addSyntax(playerOnly(this::execute), patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), flagsArg, patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), patternArg, radiusArg, heightArg);
            addSyntax(playerOnly(this::execute), flagsArg, patternArg, radiusArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var pattern = context.get(patternArg);
            var radius = (Vec) CoordinateUtil.max(context.get(radiusArg), Vec.ONE);
            var height = context.get(heightArg);

            var generator = ShapeFunctions.cylinder(player.getPosition(), pattern, new Vec(radius.x(), height, radius.y()), flags.contains(Flags.HOLLOW));

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-cyl")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class HSphere extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Vec> radiusArg = WEArgument.CommaSeparatedVec3("radius");

        private enum Flags {
            RAISE
        }

        public HSphere() {
            super("/hsphere");

            addSyntax(playerOnly(this::execute), patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), flagsArg, patternArg, radiusArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var pattern = context.get(patternArg);
            var radius = (Vec) CoordinateUtil.max(context.get(radiusArg), Vec.ONE);

            var position = player.getPosition();
            if (flags.contains(Flags.RAISE)) position = position.add(0, radius.y() - 1, 0);
            var generator = ShapeFunctions.sphere(position, pattern, radius, true);

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-sphere")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Sphere extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Vec> radiusArg = WEArgument.CommaSeparatedVec3("radius");

        private enum Flags {
            RAISE,
            HOLLOW
        }

        public Sphere() {
            super("/sphere");

            addSyntax(playerOnly(this::execute), patternArg, radiusArg);
            addSyntax(playerOnly(this::execute), flagsArg, patternArg, radiusArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var pattern = context.get(patternArg);
            var radius = (Vec) CoordinateUtil.max(context.get(radiusArg), Vec.ONE);

            var position = player.getPosition();
            if (flags.contains(Flags.RAISE)) position = position.add(0, radius.y() - 1, 0);
            var generator = ShapeFunctions.sphere(position, pattern, radius, flags.contains(Flags.HOLLOW));

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-sphere")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class HPyramid extends WECommand {
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Integer> heightArg = Argument.Int("height").clamp(1, 100);

        public HPyramid() {
            super("/hpyramid");

            addSyntax(playerOnly(this::execute), patternArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var pattern = context.get(patternArg);
            var height = context.get(heightArg);

            var generator = ShapeFunctions.pyramid(player.getPosition(), pattern, height, true);

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-hpyramid")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    public static class Pyramid extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
        private final Argument<Integer> heightArg = Argument.Int("height").clamp(1, 100);

        private enum Flags {
            HOLLOW
        }

        public Pyramid() {
            super("/pyramid");

            addSyntax(playerOnly(this::execute), patternArg, heightArg);
            addSyntax(playerOnly(this::execute), flagsArg, patternArg, heightArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var pattern = context.get(patternArg);
            var height = context.get(heightArg);

            var generator = ShapeFunctions.pyramid(player.getPosition(), pattern, height, flags.contains(Flags.HOLLOW));

            var session = LocalSession.forPlayer(player);
            session.buildTask("we-pyramid")
                    .metadata() //todo
                    .compute(generator)
                    .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                    .submit();
        }
    }

    private GenerationCommands() {

    }
}
