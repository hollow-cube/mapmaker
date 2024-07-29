package net.minestom.server.entity;

import org.jetbrains.annotations.NotNull;

public final class EntityMetadataStealer {

    public static @NotNull MetadataHolder steal(@NotNull Entity entity) {
        return entity.metadata;
    }

}
