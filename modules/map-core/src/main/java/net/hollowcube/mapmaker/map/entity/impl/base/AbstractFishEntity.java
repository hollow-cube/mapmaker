package net.hollowcube.mapmaker.map.entity.impl.base;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.AbstractFishMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractFishEntity<M extends AbstractFishMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<@NotNull AbstractFishEntity<? extends AbstractFishMeta>> INFO = MapEntityInfo.<AbstractFishEntity<? extends AbstractFishMeta>>builder(AbstractLivingEntity.INFO)
        .build();

    protected AbstractFishEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractFishEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

}
