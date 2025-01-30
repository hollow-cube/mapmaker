package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.util.transformations.SchematicTransformation;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ClipFlipCommand extends CommandDsl {

    private final Argument<String> axisArg = Argument.Word("axis").with("x", "y", "z").defaultValue("x");

    public ClipFlipCommand() {
        super("flip");

        addSyntax(playerOnly(this::handleFlip));
        addSyntax(playerOnly(this::handleFlip), axisArg);
    }

    private void handleFlip(@NotNull Player player, @NotNull CommandContext context) {
        var playerSession = PlayerSession.forPlayer(player);
        var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

        if (clipboard.isEmpty()) {
            player.sendMessage(Component.translatable("terraform.generic.empty_clipboard"));
            return;
        }

        if (context.has(axisArg)) {
            String axis = context.get(axisArg).toLowerCase(Locale.ROOT);
            switch (axis) {
                case "x" -> clipboard.transform(SchematicTransformation.flip(true, false, false));
                case "y" -> clipboard.transform(SchematicTransformation.flip(false, true, false));
                case "z" -> clipboard.transform(SchematicTransformation.flip(false, false, true));
                default -> {
                    player.sendMessage(Component.translatable("terraform.clipboard.flip.unknown_axis", Component.text(axis)));
                    return;
                }
            }
            player.sendMessage(Component.translatable("terraform.clipboard.flip", Component.text(axis)));
        }
    }
}
