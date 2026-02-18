package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapMgmtConsumerImpl extends MapMgmtConsumer {

    private final AbstractMultiMapServer server;

    public MapMgmtConsumerImpl(@NotNull JetStreamWrapper jetStream, @NotNull AbstractMultiMapServer server) {
        super(jetStream);
        this.server = server;
    }

    @Override
    protected void handleMapDelete(@NotNull String mapId) {
        server.destroyMapWorlds(mapId, Component.translatable("generic.map.closed"));
    }

    @Override
    protected void handleMapDrain(@NotNull String mapId, @Nullable String reason) {
        Component reasonComponent = reason == null
                ? Component.translatable("generic.map.closed")
                : Component.translatable("generic.map.closed.reason", Component.text(reason));
        server.destroyMapWorlds(mapId, reasonComponent);
    }
}
