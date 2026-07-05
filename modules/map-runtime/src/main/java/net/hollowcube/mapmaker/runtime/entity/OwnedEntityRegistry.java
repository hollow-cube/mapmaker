package net.hollowcube.mapmaker.runtime.entity;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.entity.OwnedEntity;
import net.hollowcube.mapmaker.map.entity.impl.projectile.EnderPearlEntity;
import net.hollowcube.mapmaker.map.entity.impl.projectile.WindChargeEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class OwnedEntityRegistry {

    private static final Map<Key, BiFunction<MapPlayer, CompoundBinaryTag, Entity>> REGISTRY;

    static {
        var registry = new HashMap<Key, BiFunction<MapPlayer, CompoundBinaryTag, Entity>>();
        registry.put(EntityType.ENDER_PEARL.key(), EnderPearlEntity::restore);
        registry.put(EntityType.WIND_CHARGE.key(), WindChargeEntity::restore);
        REGISTRY = Map.copyOf(registry);
    }

    public static <E extends Entity & OwnedEntity> @Nullable E restore(Key kind, MapPlayer owner, CompoundBinaryTag nbt) {
        var restorer = REGISTRY.get(kind);
        //noinspection unchecked
        return restorer == null ? null : (E) restorer.apply(owner, nbt);
    }

    private OwnedEntityRegistry() {
    }
}
