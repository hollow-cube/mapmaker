package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;

public class ClipRotateCommand extends Command {

    public ClipRotateCommand() {
        super("rotate");
    }

    //   //todo probably should use argument angle
    //            private final Argument<Double> xArg = ArgumentType.Double("x").setDefaultValue(90.0);
    //            private final Argument<Double> yArg = ArgumentType.Double("y").setDefaultValue(0.0);
    //            private final Argument<Double> zArg = ArgumentType.Double("z").setDefaultValue(0.0);
    //
    //            public Rotate() {
    //                super("rotate", "r");
    //
    //                setDefaultExecutor((s, c) -> s.sendMessage("default need to show help or something"));
    //
    //                addSyntax(this::handleRotate, xArg, yArg, zArg);
    //
    //                //todo multi clipboard
    ////                maybe: addSyntax(this::handleRotate2, axesArg, degreesArg);?
    //            }
    //
    //            private void handleRotate(@NotNull CommandSender sender, @NotNull CommandContext context) {
    //                if (!(sender instanceof Player player)) {
    //                    sender.sendMessage(Component.translatable("generic.players_only"));
    //                    return;
    //                }
    //
    //                double x = context.get(xArg), y = context.get(yArg), z = context.get(zArg);
    //                if (x == 0 && y == 0 && z == 0) {
    //                    player.sendMessage("no rotation");
    //                    return;
    //                }
    //
    //                var playerSession = PlayerSession.forPlayer(player);
    //                var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
    //                if (clipboard.isEmpty()) {
    //                    player.sendMessage("empty clipboard blah blah");
    //                    return;
    //                }
    //
    //                clipboard.rotate(x, y, z);
    //                player.sendMessage("rotated " + x + ", " + y + ", " + z);
    //            }
}
