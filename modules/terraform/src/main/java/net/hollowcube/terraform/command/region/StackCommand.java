package net.hollowcube.terraform.command.region;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.compute.RegionFunctions;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class StackCommand extends CommandDsl {
    private final Argument<Integer> countArg = Argument.Int("count")
            .min(1).defaultValue(1);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public StackCommand() {
        super("stack");

        addSyntax(playerOnly(this::handleStackSelection));
        addSyntax(playerOnly(this::handleStackSelection), selectionArg);
        addSyntax(playerOnly(this::handleStackSelection), countArg);
        addSyntax(playerOnly(this::handleStackSelection), countArg, selectionArg);
    }

    public void handleStackSelection(@NotNull Player player, @NotNull CommandContext context) {
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
        var generator = RegionFunctions.stack(region, direction, count);

        var session = LocalSession.forPlayer(player);
        var task = session.buildTask("stack")
                .metadata() //todo
                .compute(generator)
                .post(result -> player.sendMessage(Messages.SELECTION_STACKED.with(result.blocksChanged())))
                          .submitIfCapacity();
        if (task == null) {
            player.sendMessage(Messages.GENERIC_QUEUE_FULL);
        }
    }
}
