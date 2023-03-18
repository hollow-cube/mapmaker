package net.hollowcube.world.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

class StoneGenerator implements Generator {

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        unit.modifier().fillHeight(0, 4, Block.STONE);
    }

}
