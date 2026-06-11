package net.minestom.server.entity;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class EntityMetadataStealer {

    public static @NotNull MetadataHolder steal(@NotNull Entity entity) {
        return entity.metadata;
    }

    @Deprecated
    public static @NotNull Metadata.Entry<?> legacyReadEntry(@NotNull NetworkBuffer buffer, int type) {
        final Metadata.Type<?> metadataType = Metadata.typeById(type);
        Objects.requireNonNull(metadataType, "Unknown metadata type: " + type);
        return readEntry(buffer, metadataType);
    }

    private static <T> Metadata.Entry<T> readEntry(@NotNull NetworkBuffer buffer, @NotNull Metadata.Type<T> type) {
        return type.entry(buffer.read(type.serializer()));
    }

}
