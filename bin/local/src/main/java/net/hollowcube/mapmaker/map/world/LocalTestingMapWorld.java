package net.hollowcube.mapmaker.map.world;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LocalTestingMapWorld extends TestingMapWorld {

    private final LocalEditingMapWorld parent;

    public LocalTestingMapWorld(@NotNull LocalEditingMapWorld parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void load() {
        super.load();
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);
    }
}
