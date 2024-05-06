package net.hollowcube.mapmaker.instance.generation;

import net.hollowcube.mapmaker.map.BoxType;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public final class MapGenerators {
    private MapGenerators() {
    }

    private static final Generator VOID = new VoidGenerator();
    private static final Generator FLAT = new FlatGenerator();
    private static final Generator STONE = new StoneGenerator();
    private static final Generator BOX_STRAIGHT = new BoxGenerator(BoxType.STRAIGHT);
    private static final Generator BOX_CORNER = new BoxGenerator(BoxType.CORNER);

    public static @NotNull Generator voidWorld() {
        return VOID;
    }

    public static @NotNull Generator flatWorld() {
        return FLAT;
    }

    public static @NotNull Generator stoneWorld() {
        return STONE;
    }

    public static @NotNull Generator boxWorld(BoxType type) {
        return switch (type) {
            case STRAIGHT -> BOX_STRAIGHT;
            case CORNER -> BOX_CORNER;
        };
    }
}
