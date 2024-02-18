package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

public class MapMgmtConsumerImpl extends MapMgmtConsumer {
//    private final MapServerBase server;

    public MapMgmtConsumerImpl(@NotNull String bootstrapServers) {
        super(bootstrapServers);
    }

    @Override
    protected void handleMapDelete(@NotNull String mapId) {
//        server.worldManager().forceShutdownMap(mapId); //todo
    }
}
