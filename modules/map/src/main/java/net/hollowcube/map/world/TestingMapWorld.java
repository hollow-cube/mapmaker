package net.hollowcube.map.world;

import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.biome.BiomeContainer;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class TestingMapWorld extends AbstractMapWorld {
    private final EditingMapWorld parent;

    public TestingMapWorld(@NotNull EditingMapWorld parent) {
        super(parent.map(), parent.instance);
        this.parent = parent;
    }

    public @NotNull Pos spawnPoint() {
        return parent.spawnPoint();
    }

    @Override
    public @NotNull BiomeContainer biomes() {
        return parent.biomes(); // Always share biomes with the parent.
    }

    @Override
    void load() {
        //todo enable features?
    }

    @Override
    void close() {
        // Do not unregister instance, it is owned by the parent.

        Set.copyOf(players()).forEach(this::removePlayer);
    }
}
