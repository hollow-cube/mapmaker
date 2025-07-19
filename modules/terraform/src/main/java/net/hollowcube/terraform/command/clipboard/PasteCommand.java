package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.util.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PasteCommand extends CommandDsl {
    private final Argument<Mask> maskArg = TFArgument.Mask("mask").defaultValue(Mask.always());

    public PasteCommand() {
        super("paste");

        addSyntax(playerOnly(this::handlePasteClipboard));
        addSyntax(playerOnly(this::handlePasteClipboard), maskArg);
    }

    private void handlePasteClipboard(@NotNull Player player, @NotNull CommandContext context) {
        var mask = context.get(maskArg);

        //todo should have arg for this
        var localSession = LocalSession.forPlayer(player);
        var selection = localSession.selection(Selection.DEFAULT);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        //todo should have arg for this
        var playerSession = PlayerSession.forPlayer(player);
        var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

        execute(player, clipboard, mask);
    }

    public void execute(@NotNull Player player, @NotNull Clipboard source, @NotNull Mask sourceMask) {
        var playerPosition = player.getPosition();

        var session = LocalSession.forPlayer(player);
        var submitted = session.buildTask("paste")
                .metadata()
                .compute((task, world) -> {
                    var buffer = BlockBuffer.builder(world);
                    var schem = source.getTransformedSchematic();
                    var schemWorld = WorldView.empty(task);
                    schem.forEachBlock((p, block) -> {
                        try {
                            // Test the mask against the schematic
                            if (!sourceMask.test(schemWorld, p, block)) return;
                            buffer.set(p.add(playerPosition), block);
                        } catch (InterruptedException interrupt) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    return buffer.build();
                })
                               .submitIfCapacity();
        if (submitted == null) {
            player.sendMessage(Messages.GENERIC_QUEUE_FULL);
        }
    }

}
