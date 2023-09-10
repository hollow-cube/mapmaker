package net.hollowcube.mapmaker.object;

import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public sealed interface ObjectType permits ObjectTypeImpl {

    static @UnknownNullability ObjectType find(@NotNull String id) {
        return ObjectTypeImpl.REGISTRY.get(id);
    }

    static @NotNull Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    @NotNull String id();
    @NotNull NamespaceID namespaceId();

    int cost();

    @Nullable MapVariant requiredVariant();
    @Nullable String requiredSubVariant();

    final class Builder {
        private final @NotNull String id;
        private int cost = 1;
        private MapVariant requiredVariant = null;
        private String requiredSubVariant = null;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public @NotNull Builder cost(int cost) {
            this.cost = cost;
            return this;
        }

        public @NotNull Builder requiredVariant(@Nullable MapVariant requiredVariant) {
            this.requiredVariant = requiredVariant;
            return this;
        }

        public @NotNull Builder requiredSubVariant(@Nullable String requiredSubVariant) {
            this.requiredSubVariant = requiredSubVariant;
            return this;
        }

        public @NotNull ObjectType build() {
            var type = new ObjectTypeImpl(NamespaceID.from(id), cost, requiredVariant, requiredSubVariant);
            ObjectTypeImpl.REGISTRY.put(id, type);
            return type;
        }
    }

}
