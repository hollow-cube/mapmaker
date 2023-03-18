package net.hollowcube.world.generation;

import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public final class MapGenerators {
    private MapGenerators() {}

    private static final Generator VOID = new VoidGenerator();
    private static final Generator FLAT = new FlatGenerator();
    private static final Generator STONE = new StoneGenerator();

    public static @NotNull Generator voidWorld() {
        return VOID;
    }

    public static @NotNull Generator flatWorld() {
        return FLAT;
    }

    public static @NotNull Generator stoneWorld() {
        return STONE;
    }

}
