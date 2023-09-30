package net.hollowcube.mapmaker.object;

import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record ObjectTypeImpl(
        @NotNull NamespaceID namespaceId,
        int cost,
        @Nullable MapVariant requiredVariant,
        @Nullable String requiredSubVariant
) implements ObjectType {

    static final Map<String, ObjectType> REGISTRY = new ConcurrentHashMap<>();

    @Override
    public @NotNull String id() {
        return namespaceId.asString();
    }

}
