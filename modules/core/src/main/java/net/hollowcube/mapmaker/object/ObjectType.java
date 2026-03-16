package net.hollowcube.mapmaker.object;

import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public sealed interface ObjectType permits ObjectTypeImpl {

    static @UnknownNullability ObjectType find(String id) {
        return ObjectTypeImpl.REGISTRY.get(id);
    }

    static Builder builder(String id) {
        return new Builder(id);
    }

    String id();
    Key key();

    int cost();

    @Nullable MapVariant requiredVariant();
    @Nullable String requiredSubVariant();

    final class Builder {
        private final String id;
        private int cost = 1;
        private @Nullable MapVariant requiredVariant = null;
        private @Nullable String requiredSubVariant = null;

        private Builder(String id) {
            this.id = id;
        }

        public Builder cost(int cost) {
            this.cost = cost;
            return this;
        }

        public Builder requiredVariant(@Nullable MapVariant requiredVariant) {
            this.requiredVariant = requiredVariant;
            return this;
        }

        public Builder requiredSubVariant(@Nullable String requiredSubVariant) {
            this.requiredSubVariant = requiredSubVariant;
            return this;
        }

        public ObjectType build() {
            var type = new ObjectTypeImpl(Key.key(id), cost, requiredVariant, requiredSubVariant);
            ObjectTypeImpl.REGISTRY.put(id, type);
            return type;
        }
    }

}
