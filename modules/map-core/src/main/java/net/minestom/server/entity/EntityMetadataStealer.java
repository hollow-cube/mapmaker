package net.minestom.server.entity;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public final class EntityMetadataStealer {

    public static @NotNull MetadataHolder steal(@NotNull Entity entity) {
        return entity.metadata;
    }

    public static @NotNull NetworkBuffer.Type<Object> metadataSerializer(int type) {
        final MetadataImpl.EntryImpl<?> value = (MetadataImpl.EntryImpl<?>) MetadataImpl.EMPTY_VALUES.get(type);
        Check.notNull(value, "Unknown metadata type: " + type);
        return value == null ? null : (NetworkBuffer.Type<Object>) value.serializer();
    }

    @Deprecated
    public static @NotNull Metadata.Entry<?> legacyReadEntry(@NotNull NetworkBuffer buffer, int type) {
        final MetadataImpl.EntryImpl<?> value = (MetadataImpl.EntryImpl<?>) MetadataImpl.EMPTY_VALUES.get(type);
        Check.notNull(value, "Unknown metadata type: " + type);
        return new MetadataImpl.EntryImpl<>(type, value.serializer().read(buffer), (NetworkBuffer.Type<Object>) value.serializer());
    }

}
