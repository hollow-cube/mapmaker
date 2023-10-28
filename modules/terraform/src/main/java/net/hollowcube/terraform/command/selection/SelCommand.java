package net.hollowcube.terraform.command.selection;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.Region;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class SelCommand extends Command {
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public SelCommand() {
        super("sel");

        for (var regionType : Region.Type.values()) {
            addSubcommand(new SetTypeCommand(regionType));
        }
        addSyntax(playerOnly(this::handleClearSelection), Argument.Literal("clear"), selectionArg);
    }

    private void handleClearSelection(@NotNull Player player, @NotNull CommandContext context) {
        context.get(selectionArg).clear();
        player.sendMessage("todo cleared selection");
    }

    private final class SetTypeCommand extends Command {
        private final Region.Type regionType;

        public SetTypeCommand(@NotNull Region.Type type) {
            super(type.name().toLowerCase(Locale.ROOT));
            this.regionType = type;

            addSyntax(playerOnly(this::handleSetType), selectionArg);
        }

        private void handleSetType(@NotNull Player player, @NotNull CommandContext context) {
            context.get(selectionArg).setType(regionType);
            player.sendMessage(Component.translatable("command.terraform.sel.type.set",
                    Component.text(regionType.name().toLowerCase())));
        }
    }
}
