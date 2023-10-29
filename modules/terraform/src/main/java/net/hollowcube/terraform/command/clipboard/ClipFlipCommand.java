package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;

public class ClipFlipCommand extends Command {

    public ClipFlipCommand() {
        super("flip");
    }

    // private final Argument<Byte> axesArg = new ArgumentSwizzle("axes");
    //            private final Argument<FlipDirection> directionArg = ArgumentType.Enum("direction", FlipDirection.class)
    //                    .setFormat(ArgumentEnum.Format.LOWER_CASED)
    //                    .setDefaultValue(FlipDirection.FORWARD);
    //
    //            public Flip() {
    //                super("flip");
    //
    //                setDefaultExecutor(this::handleFlipDirection);
    //
    //                addSyntax(this::handleFlipAxes, axesArg);
    //                addSyntax(this::handleFlipDirection, directionArg);
    //            }
    //
    //            private void handleFlipAxes(@NotNull CommandSender sender, @NotNull CommandContext context) {
    //                if (!(sender instanceof Player player)) {
    //                    sender.sendMessage(Component.translatable("generic.players_only"));
    //                    return;
    //                }
    //
    //                handleFlip(player, context.get(axesArg));
    //            }
    //
    //            private void handleFlipDirection(@NotNull CommandSender sender, @NotNull CommandContext context) {
    //                if (!(sender instanceof Player player)) {
    //                    sender.sendMessage(Component.translatable("generic.players_only"));
    //                    return;
    //                }
    //
    //                var flipDirection = context.get(directionArg);
    //                handleFlip(player, flipDirection.axisFromView(player.getPosition().yaw()));
    //            }
    //
    //            private void handleFlip(@NotNull Player player, byte magicAxes) {
    //                var playerSession = PlayerSession.forPlayer(player);
    //                var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
    //                if (clipboard.isEmpty()) {
    //                    player.sendMessage("empty clipboard blah blah");
    //                    return;
    //                }
    //
    //                clipboard.flip(
    //                        (magicAxes & ArgumentSwizzle.X) != 0,
    //                        (magicAxes & ArgumentSwizzle.Y) != 0,
    //                        (magicAxes & ArgumentSwizzle.Z) != 0
    //                );
    //                player.sendMessage("flipped");
    //            }
    //
    //            private enum FlipDirection {
    //                // Absolute
    //                NORTH,
    //                SOUTH,
    //                EAST,
    //                WEST,
    //                UP,
    //                DOWN,
    //                TOP,
    //                BOTTOM,
    //
    //                // Relative
    //                BACK,
    //                FORWARD,
    //                LEFT,
    //                RIGHT;
    //
    //                public @MagicConstant(valuesFromClass = ArgumentSwizzle.class) byte axisFromView(float yaw) {
    //                    var normalYaw = yaw % 360;
    //                    if (normalYaw < 0) normalYaw += 360;
    //                    var facingX = MathUtils.isBetween(normalYaw, 45, 135) ||
    //                            MathUtils.isBetween(normalYaw, 225, 315);
    //
    //                    return switch (this) {
    //                        case NORTH, SOUTH -> ArgumentSwizzle.Z;
    //                        case EAST, WEST -> ArgumentSwizzle.X;
    //                        case UP, DOWN, TOP, BOTTOM -> ArgumentSwizzle.Y;
    //                        // 45, 135, 225, 315
    //                        case BACK, FORWARD -> facingX ? ArgumentSwizzle.X : ArgumentSwizzle.Z;
    //                        case LEFT, RIGHT -> facingX ? ArgumentSwizzle.Z : ArgumentSwizzle.X;
    //                    };
    //                }
    //            }
    //        }
}
