package net.hollowcube.map.object;

import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public sealed interface ObjectType permits ObjectTypeImpl {

    static @NotNull ObjectType create(@NotNull String id) {
        return new ObjectTypeImpl(id);
    }

    @NotNull String id();
    @NotNull NamespaceID namespaceId();

}
