package net.hollowcube.mapmaker.map.command.utility;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FixTheDripleafCommand extends CommandDsl {

    @Inject
    public FixTheDripleafCommand() {
        super("fixthedripleaf");

        addSyntax(playerOnly(this::fixTheDripleaf));
    }

    private void fixTheDripleaf(@NotNull Player player, @NotNull CommandContext context) {
        var instance = player.getInstance();
        var dimensionHeight = instance.getDimensionType().getHeight();
        player.sendMessage("Fixing the dripleaf!!!");
        int fixed = 0;
        for (var chunk : instance.getChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -64; y < dimensionHeight; y++) {
                        var block = chunk.getBlock(x, y, z);
                        if (block.name().equals("minecraft:big_dripleaf")) {
                            chunk.setBlock(x, y, z, block.withHandler(DripleafBlock.INSTANCE));
                            fixed++;
                        }
                    }
                }
            }
        }
        player.sendMessage("Fixed " + fixed + " dripleaf blocks!");
    }
}
