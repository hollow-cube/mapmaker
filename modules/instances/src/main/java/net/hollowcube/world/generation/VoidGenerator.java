package net.hollowcube.world.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public class VoidGenerator implements Generator {
    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Minestom worlds are void by default, so don't do anything except place one block
        if (unit.absoluteStart().chunkX() == 0 && unit.absoluteStart().chunkZ() == 0)
            unit.modifier().setBlock(0, 30, 0, Block.BEDROCK);
    }
}
