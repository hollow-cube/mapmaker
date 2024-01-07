package net.hollowcube.terraform.command.region;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SetCommand extends CommandDsl {
    private final Argument2<Pattern> patternArg;
    private final Argument2<Selection> selectionArg = TFArgument.Selection("selection");

    public SetCommand(@NotNull Terraform tf) {
        super("set");

        this.patternArg = TFArgument.Pattern("pattern", tf);

        addSyntax(playerOnly(this::handleSetRegionToPattern), patternArg, selectionArg);
    }

    public void handleSetRegionToPattern(@NotNull Player player, @NotNull CommandContext context) {
        var selection = context.get(selectionArg);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        var pattern = context.get(patternArg);

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("set")
                .metadata() //todo
                .compute(ComputeFunc.set(region, pattern))
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.selection.set", Component.translatable(String.valueOf(result.blocksChanged()))));
                })
                .submit();

    }
}
