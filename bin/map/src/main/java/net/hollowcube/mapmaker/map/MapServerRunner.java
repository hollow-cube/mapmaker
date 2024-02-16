package net.hollowcube.mapmaker.map;

import net.hollowcube.map.runtime.ServerBridge;
import net.hollowcube.map2.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import org.jetbrains.annotations.NotNull;

public class MapServerRunner extends AbstractMapServer {
    MapServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);
    }

    @Override
    public @NotNull ServerBridge bridge() {
        return null;
    }
}
