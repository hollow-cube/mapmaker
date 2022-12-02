package net.hollowcube.world.creation.chunkgenerator;

import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public abstract class ChunkGeneratorBase implements Generator {
    public abstract void generate(@NotNull GenerationUnit unit);
}
