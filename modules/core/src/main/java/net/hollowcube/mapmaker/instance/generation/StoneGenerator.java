package net.hollowcube.mapmaker.instance.generation;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

class StoneGenerator implements Generator {

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(0, 4, Block.STONE);
    }

}
