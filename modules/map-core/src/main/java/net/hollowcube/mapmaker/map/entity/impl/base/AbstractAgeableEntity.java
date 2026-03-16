package net.hollowcube.mapmaker.map.entity.impl.base;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.AgeableMobMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractAgeableEntity<M extends AgeableMobMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<@NotNull AbstractAgeableEntity<? extends AgeableMobMeta>> INFO = MapEntityInfo.<AbstractAgeableEntity<? extends AgeableMobMeta>>builder(AbstractLivingEntity.INFO)
        .with("Is Baby", MapEntityInfoType.Bool(false, AgeableMobMeta::setBaby, AgeableMobMeta::isBaby))
        .build();

    protected AbstractAgeableEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractAgeableEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

}
