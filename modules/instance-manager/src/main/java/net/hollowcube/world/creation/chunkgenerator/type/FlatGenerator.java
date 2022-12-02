package net.hollowcube.world.creation.chunkgenerator.type;

import net.hollowcube.world.creation.chunkgenerator.ChunkGeneratorBase;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.UnitModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FlatGenerator extends ChunkGeneratorBase {
    @Override
    public void generate(@NotNull GenerationUnit unit) {
        UnitModifier modifier = unit.modifier();
        for (byte x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
            for (byte z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                modifier.setBlock(x, 0, z, Block.BEDROCK);
                modifier.setBlock(x, 1, z, Block.DIRT);
                modifier.setBlock(x, 2, z, Block.DIRT);
                modifier.setBlock(x, 3, z, Block.DIRT);
                modifier.setBlock(x, 4, z, Block.DIRT);
                modifier.setBlock(x, 5, z, Block.GRASS_BLOCK);
            }
        }
    }

    @Override
    public void generateAll(@NotNull Collection<@NotNull GenerationUnit> units) {
        super.generateAll(units);
    }
}
