package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.ParchedMeta;

import java.util.UUID;

public class ParchedEntity extends AbstractSkeletonEntity<ParchedMeta> {

    public static final MapEntityInfo<ParchedEntity> INFO = MapEntityInfo.<ParchedEntity>builder(AbstractSkeletonEntity.INFO)
        .build();

    public ParchedEntity(UUID uuid) {
        super(EntityType.PARCHED, uuid);
    }
}
