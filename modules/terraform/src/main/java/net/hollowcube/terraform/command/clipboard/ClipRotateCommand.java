package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.types.Axis;
import net.hollowcube.schem.Rotation;
import net.hollowcube.terraform.command.util.ArgumentRotation;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.util.transformations.SchematicTransformation;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ClipRotateCommand extends CommandDsl {

    private final Argument<String> axisArg = Argument.Word("axis").with("x", "y", "z").defaultValue("x");
    private final Argument<Rotation> amountArg = ArgumentRotation.of("rotation", Rotation.NONE).defaultValue(Rotation.CLOCKWISE_90);

    public ClipRotateCommand() {
        super("rotate");

        addSyntax(playerOnly(this::handleRotate));
        addSyntax(playerOnly(this::handleRotate), axisArg);
        addSyntax(playerOnly(this::handleRotate), axisArg, amountArg);
    }

    private void handleRotate(@NotNull Player player, @NotNull CommandContext context) {
        var playerSession = PlayerSession.forPlayer(player);
        var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

        if (clipboard.isEmpty()) {
            player.sendMessage(Component.translatable("terraform.generic.empty_clipboard"));
            return;
        }

        if (context.has(axisArg) && context.has(amountArg)) {
            Rotation rotation = context.get(amountArg);
            if (rotation == Rotation.NONE) {
                player.sendMessage(Component.translatable("terraform.clipboard.rotate.invalid"));
                // no rotation? Insert megamind meme
                return;
            }
            String axis = context.get(axisArg).toLowerCase(Locale.ROOT);

            switch (axis) {
                case "x" -> clipboard.transform(SchematicTransformation.rotate(Axis.X, rotation));
                case "y" -> clipboard.transform(SchematicTransformation.rotate(Axis.Y, rotation));
                case "z" -> clipboard.transform(SchematicTransformation.rotate(Axis.Z, rotation));
                default -> {
                    player.sendMessage(Component.translatable("terraform.clipboard.rotate.unknown_axis", Component.text(axis)));
                    return;
                }
            }
            player.sendMessage(Component.translatable("terraform.clipboard.rotate", Component.text(axis), Component.text(rotation.toDegrees())));
        }
    }
}