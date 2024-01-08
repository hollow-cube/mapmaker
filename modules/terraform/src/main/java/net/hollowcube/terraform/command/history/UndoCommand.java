package net.hollowcube.terraform.command.history;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UndoCommand extends CommandDsl {
    private final Argument<Integer> countArg = Argument.Int("count").min(1).defaultValue(1);

    public UndoCommand() {
        super("undo");

        addSyntax(playerOnly(this::handleUndo));
        addSyntax(playerOnly(this::handleUndo), countArg);
    }

    private void handleUndo(@NotNull Player player, @NotNull CommandContext context) {
        int targetCount = context.get(countArg);

        var session = LocalSession.forPlayer(player);
        int undone = session.undo(targetCount);
        player.sendMessage(Component.translatable("terraform.history.undo", Component.translatable(String.valueOf(undone))));
    }
}
