package net.hollowcube.mapmaker.isolate;

import net.hollowcube.mapmaker.map.MapMgmtConsumer;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

class MapIsolateMapMgmtConsumerImpl extends MapMgmtConsumer {

    private final MapIsolateServer server;

    public MapIsolateMapMgmtConsumerImpl(JetStreamWrapper jetStream, MapIsolateServer server) {
        super(jetStream);
        this.server = server;
    }

    @Override
    protected void handleMapDelete(String mapId) {
        if (!server.mapId().equals(mapId)) return;
        server.shutdown(Component.translatable("generic.map.closed"));
    }

    @Override
    protected void handleMapDrain(String mapId, @Nullable String reason) {
        if (!server.mapId().equals(mapId)) return;
        Component reasonComponent = reason == null
            ? Component.translatable("generic.map.closed")
            : Component.translatable("generic.map.closed.reason", Component.text(reason));
        server.shutdown(reasonComponent);
    }

}
