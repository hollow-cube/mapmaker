package net.hollowcube.terraform.command.region;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReplaceCommand extends Command {
    private final Argument<Mask> maskArg = TFArgument.Mask("mask");
    private final Argument<Pattern> patternArg = TFArgument.Pattern("pattern");
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public ReplaceCommand() {
        super("replace");

        addSyntax(playerOnly(this::handleReplaceMaskInRegion), maskArg, patternArg, selectionArg);
    }

    public void handleReplaceMaskInRegion(@NotNull Player player, @NotNull CommandContext context) {
        var mask = context.get(maskArg);
        var pattern = context.get(patternArg);

        var selection = context.get(selectionArg);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("replace")
                .metadata() //todo
                .compute(ComputeFunc.replace(region, mask, pattern))
                .submit();
    }
}
