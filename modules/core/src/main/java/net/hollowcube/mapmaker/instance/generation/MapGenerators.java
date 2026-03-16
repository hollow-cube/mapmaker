package net.hollowcube.mapmaker.instance.generation;

import net.minestom.server.instance.generator.Generator;

public final class MapGenerators {
    private MapGenerators() {
    }

    private static final Generator VOID = new VoidGenerator();
    private static final Generator FLAT = new FlatGenerator();
    private static final Generator STONE = new StoneGenerator();

    public static Generator voidWorld() {
        return VOID;
    }

    public static Generator flatWorld() {
        return FLAT;
    }

    public static Generator stoneWorld() {
        return STONE;
    }

}
