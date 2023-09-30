package net.hollowcube.map;

import net.hollowcube.mapmaker.map.MapMgmtConsumer;
import org.jetbrains.annotations.NotNull;

public class MapMgmtConsumerImpl extends MapMgmtConsumer {
    private final MapServerBase server;

    public MapMgmtConsumerImpl(@NotNull String bootstrapServers, @NotNull MapServerBase server) {
        super(bootstrapServers);
        this.server = server;
    }

    @Override
    protected void handleMapDelete(@NotNull String mapId) {
        server.worldManager().forceShutdownMap(mapId);
    }
}
