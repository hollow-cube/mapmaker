package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.util.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class CutCommand extends CommandDsl {

    public CutCommand() {
        super("cut");

        addSyntax(playerOnly(this::handleCutSelection));
    }

    private void handleCutSelection(@NotNull Player player, @NotNull CommandContext context) {
        var playerSession = PlayerSession.forPlayer(player);
        var localSession = LocalSession.forPlayer(player);
        var pos = player.getPosition();

        //todo should have arg for this
        var selection = localSession.selection(Selection.DEFAULT);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        //todo should have arg for this
        var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

        // Take away the blocks, then add the undo batch to the clipboard
        var task = localSession.buildTask("cut")
                .metadata() //todo
                .compute(ComputeFunc.set(region, Pattern.block(Block.AIR)))
                .post(result -> {
                    player.sendMessage(Component.translatable("terraform.cut", Component.translatable(String.valueOf(result.blocksChanged()))));
                    clipboard.setData(result.undoBuffer().toSchematic(new Vec(-pos.blockX(), -pos.blockY(), -pos.blockZ())));
                })
                               .submitIfCapacity();
        if (task == null) {
            player.sendMessage(Messages.GENERIC_QUEUE_FULL);
        }
    }
}
