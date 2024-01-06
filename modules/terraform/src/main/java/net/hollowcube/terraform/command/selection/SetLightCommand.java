package net.hollowcube.terraform.command.selection;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.util.LightUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.ChunkHack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class SetLightCommand extends Command {
    private final Argument<Integer> levelArg = Argument.Int("level").clamp(0, 15);
    private final Argument<Selection> selectionArg = TFArgument.Selection("selection");

    public SetLightCommand() {
        super("setlight");

        addSyntax(playerOnly(this::handleSetLight), levelArg, selectionArg);
    }

    private void handleSetLight(@NotNull Player player, @NotNull CommandContext context) {
        int level = context.get(levelArg);

        var selection = context.get(selectionArg);
        var region = selection.region();
        if (region == null) {
            player.sendMessage("No region selected");
            return;
        }

        var modifiedChunks = new HashSet<Chunk>();
        var instance = player.getInstance();
        for (var point : region) {
            var chunk = instance.getChunkAt(point);
            if (chunk == null) continue;

            modifiedChunks.add(chunk);
            var section = chunk.getSectionAt(point.blockY());

            var blockLight = section.blockLight();
            LightUtil.setLevel(blockLight, point.blockX() & 15, point.blockY() & 15, point.blockZ() & 15, (byte) level);
        }

        for (var chunk : modifiedChunks) {
            ChunkHack.invalidateChunk(chunk);
            chunk.sendChunk();
        }

        player.sendMessage("Set light level to " + level + " in " + modifiedChunks.size() + " chunks");
    }
}
