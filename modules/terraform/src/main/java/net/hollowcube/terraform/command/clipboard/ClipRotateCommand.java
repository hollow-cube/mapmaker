package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ClipRotateCommand extends CommandDsl {

    private final Argument<String> axisArg = Argument.Word("axis").with("x", "y", "z").defaultValue("x");
    private final Argument<Integer> amountArg = Argument.Int("rotation").clamp(0, 360).defaultValue(90);

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
            int amount = context.get(amountArg);
            if (amount == 0 || amount == 360) {
                player.sendMessage(Component.translatable("terraform.clipboard.rotate.invalid"));
                // no rotation? Insert megamind meme
                return;
            }
            String axis = context.get(axisArg).toLowerCase(Locale.ROOT);
            switch (axis) {
                case "x" -> clipboard.rotate(amount, 0, 0);
                case "y" -> clipboard.rotate(0, amount, 0);
                case "z" -> clipboard.rotate(0, 0, amount);
                default -> {
                    player.sendMessage(Component.translatable("terraform.clipboard.rotate.unknown_axis", Component.text(axis)));
                    return;
                }
            }
            player.sendMessage(Component.translatable("terraform.clipboard.rotate", Component.text(axis), Component.text(amount)));
        }
    }
}