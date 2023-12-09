package net.hollowcube.mapmaker.instance.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public class VoidGenerator implements Generator {
    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Minestom worlds are void by default, so don't do anything except place one block
        if (unit.absoluteStart().chunkX() == 0 && unit.absoluteStart().chunkZ() == 0)
            // Fill in 3x3 grid
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    unit.modifier().setBlock(x, 39, z, Block.STONE);
                }
            }
    }
}
