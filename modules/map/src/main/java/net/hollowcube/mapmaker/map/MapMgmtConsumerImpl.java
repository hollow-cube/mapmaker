package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.runtime.LocalMapAllocator;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapMgmtConsumerImpl extends MapMgmtConsumer {
    private final LocalMapAllocator allocator;

    public MapMgmtConsumerImpl(@NotNull LocalMapAllocator allocator, @NotNull String bootstrapServers) {
        super(bootstrapServers);
        this.allocator = allocator;
    }

    @Override
    protected void handleMapDelete(@NotNull String mapId) {
        allocator.destroy(mapId, Component.translatable("generic.map.closed"));
    }

    @Override
    protected void handleMapDrain(@NotNull String mapId, @Nullable String reason) {
        Component reasonComponent = reason == null
                ? Component.translatable("generic.map.closed")
                : Component.translatable("generic.map.closed.reason", Component.text(reason));
        allocator.destroyAll(mapId, reasonComponent);
    }
}
