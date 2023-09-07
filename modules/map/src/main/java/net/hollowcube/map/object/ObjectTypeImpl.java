package net.hollowcube.map.object;

import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

final class ObjectTypeImpl implements ObjectType {

    private final NamespaceID id;

    public ObjectTypeImpl(@NotNull String id) {
        this.id = NamespaceID.from(id);
    }

    @Override
    public @NotNull String id() {
        return id.asString();
    }

    @Override
    public @NotNull NamespaceID namespaceId() {
        return id;
    }

}
