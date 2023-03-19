package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

public class TestingMapWorld extends PlayingMapWorld {
    public TestingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);
    }
}
