package net.hollowcube.terraform.command.selection;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.selection.Selection;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed abstract class PosCommand extends Command permits PosCommand.Primary, PosCommand.Secondary {
    private final Argument<@Nullable Point> posArg = Argument.Opt(Argument.RelativeVec3("pos"));
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    private PosCommand(@NotNull String name) {
        super(name);

        addSyntax(playerOnly(this::handleSelectionUpdate), posArg, selectionArg);
    }

    private void handleSelectionUpdate(@NotNull Player player, @NotNull CommandContext context) {
        // Determine the target position
        var point = context.get(posArg);
        if (point == null) {
            point = player.getPosition();
        }

        // Get the selection (or default handled by the argument type)
        var selection = context.get(selectionArg);

        performSelection(player, selection, point);
    }

    protected abstract void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point);

    public static final class Primary extends PosCommand {
        public Primary() {
            super("pos1");
        }

        @Override
        protected void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point) {
            var changed = selection.selectPrimary(point, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos1.already_set"));
            }
        }
    }

    public static final class Secondary extends PosCommand {
        public Secondary() {
            super("pos2");
        }

        @Override
        protected void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point) {
            var changed = selection.selectSecondary(point, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos2.already_set"));
            }
        }
    }

}
