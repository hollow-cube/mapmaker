package net.hollowcube.terraform.command.region;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MoveCommand extends CommandDsl {

    private final Argument<Integer> countArg = Argument.Int("count")
            .min(1).defaultValue(1);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public MoveCommand() {
        super("move");

        addSyntax(playerOnly(this::handleMoveSelection));
        addSyntax(playerOnly(this::handleMoveSelection), selectionArg);
        addSyntax(playerOnly(this::handleMoveSelection), countArg);
        addSyntax(playerOnly(this::handleMoveSelection), countArg, selectionArg);
    }

    private void handleMoveSelection(@NotNull Player player, @NotNull CommandContext context) {
        var selection = context.get(selectionArg);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        var count = context.get(countArg);
        if (count < 1 || count > 100) {
            player.sendMessage(Component.translatable("generic.number.not_in_range", Component.text(1), Component.text(100)));
            return;
        }

        var direction = DirectionUtil.fromView(player.getPosition());
        var generator = RegionFunctions.move(region, direction, count);

        // Execute the change
        var session = LocalSession.forPlayer(player);
        session.buildTask("move")
                .metadata() //todo
                .compute(generator)
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.selection.move",
                            Component.translatable(String.valueOf(result.blocksChanged()))));
                    // Move selection to match the move command
                    // builders don't like this behavior. Should be an optional flag
//                    Point newPrimary = selection.region().min().add(offset);
//                    Point newSecondary = selection.region().max().add(offset);
//                    selection.selectPrimary(newPrimary, false);
//                    selection.selectSecondary(newSecondary, false);
                })
                .submit();
    }
}
