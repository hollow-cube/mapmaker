package net.hollowcube.world.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.generator.UnitModifier;
import org.jetbrains.annotations.NotNull;

class FlatGenerator implements Generator {

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        UnitModifier modifier = unit.modifier();
        modifier.fillHeight(0, 1, Block.BEDROCK);
        modifier.fillHeight(1, 5, Block.DIRT);
        modifier.fillHeight(5, 6, Block.GRASS_BLOCK);
    }

}
