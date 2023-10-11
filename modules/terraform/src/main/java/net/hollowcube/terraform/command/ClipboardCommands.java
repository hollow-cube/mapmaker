package net.hollowcube.terraform.command;

import net.hollowcube.terraform.command.helper.ArgumentSwizzle;
import net.hollowcube.terraform.command.helper.ExtraArguments;
import net.hollowcube.terraform.give_me_new_home.instance.SchemBlockBatch;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.MathUtils;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClipboardCommands {
    private ClipboardCommands() {
    }

    public static final class Copy extends Command {

        public Copy(@Nullable CommandCondition condition) {
            super("copy", "tf:copy");
            setCondition(condition); //todo support from and to

            setDefaultExecutor(this::handleCopySelection);
        }

        private void handleCopySelection(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            // Determine the target selection
            //todo should have arg for this
            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            var region = selection.region();
            if (region == null) {
                sender.sendMessage(Component.translatable("command.terraform.no_selection"));
                return;
            }

            // Determine the target clipboard
            //todo should have arg for this
            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

            session.action()
                    .at(player.getPosition())
                    .from(region)
                    .toSchematic(schem -> {
                        clipboard.setData(schem);
                        sender.sendMessage(Component.translatable("command.terraform.copy.success"));
                    });
        }
    }

    public static final class Cut extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Cut(@Nullable CommandCondition condition) {
            super("cut", "tf:cut");
            setCondition(condition);

            setDefaultExecutor(this::handleCutSelection);
            addSyntax(this::handleCutSelection, selectionArg);
        }

        private void handleCutSelection(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            // Determine the target selection
            //todo should have arg for this
            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            var region = selection.region();
            if (region == null) {
                sender.sendMessage(Component.translatable("command.terraform.no_selection"));
                return;
            }

            // Determine the target clipboard
            //todo should have arg for this
            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

            session.action()
                    .at(player.getPosition())
                    .from(region)
                    .toSchematic(schem -> {
                        clipboard.setData(schem);
                        sender.sendMessage(Component.translatable("command.terraform.copy.success"));
                    });

            var batch = new SchemBlockBatch();
            region.forEach(point -> batch.setBlock(point, Block.AIR));
            batch.apply(player.getInstance()).join(); //todo do not join, use other systems, etc
        }
    }

    public static final class Paste extends Command {

        public Paste(@Nullable CommandCondition condition) {
            super("paste", "tf:paste");
            setCondition(condition);

            setDefaultExecutor(this::handlePasteClipboard);
        }

        private void handlePasteClipboard(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            // Determine the target clipboard
            //todo should have arg for this
            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
            if (clipboard.isEmpty()) {
                sender.sendMessage(Component.translatable("command.terraform.paste.empty"));
                return;
            }

            var session = LocalSession.forPlayer(player);
            clipboard.apply(session, player.getPosition())
                    .thenAccept(unused -> player.sendMessage(Component.translatable("command.terraform.paste.success")));
        }
    }

    public static final class ClipboardCommand extends Command {

        public ClipboardCommand(@Nullable CommandCondition condition) {
            super("clipboard", "c", "tf:clipboard");
            setCondition(condition);

            addSubcommand(new List());
            addSubcommand(new Clear());
            addSubcommand(new Rotate());
            addSubcommand(new Flip());
        }

        static final class List extends Command {
            public List() {
                super("list", "l");

                setDefaultExecutor(this::showClipboardList);
            }

            private void showClipboardList(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                var session = PlayerSession.forPlayer(player);
                var clipboards = session.clipboardNames();

                if (clipboards.isEmpty()) {
                    player.sendMessage("no clipboards");
                    return;
                }

                for (var clipboard : session.clipboardNames()) {
                    player.sendMessage(clipboard);
                }
            }
        }

        static final class Clear extends Command {
            private final Argument<Clipboard> clipboardArg = ExtraArguments.Clipboard("clipboard");

            public Clear() {
                super("clear", "c");

                setDefaultExecutor(this::showErrorTodo);
                addSyntax(this::handleClearClipboard, clipboardArg);
                addSyntax(this::handleClearClipboard);
            }

            private void handleClearClipboard(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                // Use the argument, or the default clipboard
                var clipboard = context.get(clipboardArg);
                if (clipboard == null) {
                    var session = PlayerSession.forPlayer(player);
                    clipboard = session.clipboard(Clipboard.DEFAULT);
                }

                // Clear the clipboard
                clipboard.setData(null);
            }

            private void showErrorTodo(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                player.sendMessage("I need to show an error :)");
            }
        }

        static final class Rotate extends Command {
            //todo probably should use argument angle
            private final Argument<Double> xArg = ArgumentType.Double("x").setDefaultValue(90.0);
            private final Argument<Double> yArg = ArgumentType.Double("y").setDefaultValue(0.0);
            private final Argument<Double> zArg = ArgumentType.Double("z").setDefaultValue(0.0);

            public Rotate() {
                super("rotate", "r");

                setDefaultExecutor((s, c) -> s.sendMessage("default need to show help or something"));

                addSyntax(this::handleRotate, xArg, yArg, zArg);

                //todo multi clipboard
//                maybe: addSyntax(this::handleRotate2, axesArg, degreesArg);?
            }

            private void handleRotate(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                double x = context.get(xArg), y = context.get(yArg), z = context.get(zArg);
                if (x == 0 && y == 0 && z == 0) {
                    player.sendMessage("no rotation");
                    return;
                }

                var playerSession = PlayerSession.forPlayer(player);
                var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
                if (clipboard.isEmpty()) {
                    player.sendMessage("empty clipboard blah blah");
                    return;
                }

                clipboard.rotate(x, y, z);
                player.sendMessage("rotated " + x + ", " + y + ", " + z);
            }
        }

        static final class Flip extends Command {
            private final Argument<Byte> axesArg = new ArgumentSwizzle("axes");
            private final Argument<FlipDirection> directionArg = ArgumentType.Enum("direction", FlipDirection.class)
                    .setFormat(ArgumentEnum.Format.LOWER_CASED)
                    .setDefaultValue(FlipDirection.FORWARD);

            public Flip() {
                super("flip");

                setDefaultExecutor(this::handleFlipDirection);

                addSyntax(this::handleFlipAxes, axesArg);
                addSyntax(this::handleFlipDirection, directionArg);
            }

            private void handleFlipAxes(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                handleFlip(player, context.get(axesArg));
            }

            private void handleFlipDirection(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                var flipDirection = context.get(directionArg);
                handleFlip(player, flipDirection.axisFromView(player.getPosition().yaw()));
            }

            private void handleFlip(@NotNull Player player, byte magicAxes) {
                var playerSession = PlayerSession.forPlayer(player);
                var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
                if (clipboard.isEmpty()) {
                    player.sendMessage("empty clipboard blah blah");
                    return;
                }

                clipboard.flip(
                        (magicAxes & ArgumentSwizzle.X) != 0,
                        (magicAxes & ArgumentSwizzle.Y) != 0,
                        (magicAxes & ArgumentSwizzle.Z) != 0
                );
                player.sendMessage("flipped");
            }

            private enum FlipDirection {
                // Absolute
                NORTH,
                SOUTH,
                EAST,
                WEST,
                UP,
                DOWN,
                TOP,
                BOTTOM,

                // Relative
                BACK,
                FORWARD,
                LEFT,
                RIGHT;

                public @MagicConstant(valuesFromClass = ArgumentSwizzle.class) byte axisFromView(float yaw) {
                    var normalYaw = yaw % 360;
                    if (normalYaw < 0) normalYaw += 360;
                    var facingX = MathUtils.isBetween(normalYaw, 45, 135) ||
                            MathUtils.isBetween(normalYaw, 225, 315);

                    return switch (this) {
                        case NORTH, SOUTH -> ArgumentSwizzle.Z;
                        case EAST, WEST -> ArgumentSwizzle.X;
                        case UP, DOWN, TOP, BOTTOM -> ArgumentSwizzle.Y;
                        // 45, 135, 225, 315
                        case BACK, FORWARD -> facingX ? ArgumentSwizzle.X : ArgumentSwizzle.Z;
                        case LEFT, RIGHT -> facingX ? ArgumentSwizzle.Z : ArgumentSwizzle.X;
                    };
                }
            }
        }
    }

}
