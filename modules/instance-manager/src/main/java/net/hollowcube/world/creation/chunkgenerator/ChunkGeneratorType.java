package net.hollowcube.world.creation.chunkgenerator;

import net.hollowcube.world.creation.chunkgenerator.type.FlatGenerator;
import net.hollowcube.world.creation.chunkgenerator.type.VoidGenerator;
import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.generator.Generator;

import java.lang.reflect.InvocationTargetException;

public enum ChunkGeneratorType {
    VOID(VoidGenerator.class),
    FLAT(FlatGenerator.class);

    private final Class<? extends Generator> chunkGenerator;

    ChunkGeneratorType(Class<? extends Generator> chunkGenerator) {
        this.chunkGenerator = chunkGenerator;
    }

    public Generator getChunkGenerator() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return this.chunkGenerator.getDeclaredConstructor().newInstance();
    }
}
