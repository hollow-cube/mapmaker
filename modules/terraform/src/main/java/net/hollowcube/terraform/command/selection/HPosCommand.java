package net.hollowcube.terraform.command.selection;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public sealed abstract class HPosCommand extends CommandDsl permits HPosCommand.Primary, HPosCommand.Secondary {
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    protected HPosCommand(@NotNull String name) {
        super(name);

        addSyntax(playerOnly(this::handleSelectionUpdate));
        addSyntax(playerOnly(this::handleSelectionUpdate), selectionArg);
    }

    private void handleSelectionUpdate(@NotNull Player player, @NotNull CommandContext context) {
        // Determine the target position
        var targetBlock = PlayerUtil.getTargetBlock(player, 512);
        if (targetBlock == null) {
            player.sendMessage(Component.translatable("command.terraform.hpos.no_block"));
            return;
        }

        // Get the selection (or default handled by the argument type)
        var selection = context.get(selectionArg);

        performSelection(player, selection, targetBlock);
    }

    protected abstract void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point);

    public static final class Primary extends HPosCommand {
        public Primary() {
            super("hpos1");
        }

        @Override
        protected void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point) {
            var changed = selection.selectPrimary(point, true);
            if (!changed) {
                player.sendMessage(Component.translatable("terraform.pos1.already_set"));
            }
        }
    }

    public static final class Secondary extends HPosCommand {
        public Secondary() {
            super("hpos2");
        }

        @Override
        protected void performSelection(@NotNull Player player, @NotNull Selection selection, @NotNull Point point) {
            var changed = selection.selectSecondary(point, true);
            if (!changed) {
                player.sendMessage(Component.translatable("terraform.pos2.already_set"));
            }
        }
    }
}
