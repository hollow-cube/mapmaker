package net.hollowcube.world.creation.chunkgenerator.type;

import net.hollowcube.world.creation.chunkgenerator.ChunkGeneratorBase;
import net.minestom.server.instance.generator.GenerationUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class VoidGenerator extends ChunkGeneratorBase {
    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Minestom worlds are void by default, so don't do anything
    }

    @Override
    public void generateAll(@NotNull Collection<@NotNull GenerationUnit> units) {
        super.generateAll(units);
    }
}
