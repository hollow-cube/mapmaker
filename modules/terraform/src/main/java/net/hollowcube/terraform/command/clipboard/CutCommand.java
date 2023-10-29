package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.task.ComputeFunc;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class CutCommand extends Command {

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
        localSession.buildTask("cut")
                .metadata() //todo
                .compute(ComputeFunc.set(region, Pattern.block(Block.AIR)))
                .post(result -> {
                    player.sendMessage("cut " + result.blocksChanged() + " blocks to clipboard");
                    clipboard.setData(result.undoBuffer().toSchematic(new Vec(pos.blockX(), pos.blockY(), pos.blockZ()).mul(-1)));
                })
                .submit();
    }
}
