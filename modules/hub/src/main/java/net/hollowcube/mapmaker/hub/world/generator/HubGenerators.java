package net.hollowcube.mapmaker.hub.world.generator;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public final class HubGenerators {
    private HubGenerators() {
    }

    //todo move me to
    private static final Generator STONE = unit -> {
        unit.modifier().fillHeight(0, 4, Block.STONE);
    };

    public static @NotNull Generator stoneWorld() {
        return STONE;
    }
}
