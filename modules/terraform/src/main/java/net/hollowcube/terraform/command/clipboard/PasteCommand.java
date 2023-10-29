package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.schem.Rotation;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PasteCommand extends Command {

    public PasteCommand() {
        super("paste");

        addSyntax(playerOnly(this::handlePasteClipboard));
    }

    private void handlePasteClipboard(@NotNull Player player, @NotNull CommandContext context) {
        var playerSession = PlayerSession.forPlayer(player);
        var localSession = LocalSession.forPlayer(player);
        var playerPosition = player.getPosition();

        //todo should have arg for this
        var selection = localSession.selection(Selection.DEFAULT);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        //todo should have arg for this
        var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

        //clipboard.getAsBuffer()
        localSession.buildTask("paste")
                .metadata()
                .compute(world -> {
                    var buffer = BlockBuffer.builder();
                    clipboard.getSchematic().apply(Rotation.NONE, (p, block) -> {
                        buffer.set(p.add(playerPosition), block.stateId());
                    });
                    return buffer.build();
                })
                .submit();
    }

}
