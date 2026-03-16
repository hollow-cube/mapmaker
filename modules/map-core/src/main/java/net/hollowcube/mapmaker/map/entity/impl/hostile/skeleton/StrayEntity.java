package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.ParchedMeta;
import net.minestom.server.entity.metadata.monster.skeleton.StrayMeta;

import java.util.UUID;

public class StrayEntity extends AbstractSkeletonEntity<StrayMeta> {

    public static final MapEntityInfo<StrayEntity> INFO = MapEntityInfo.<StrayEntity>builder(AbstractSkeletonEntity.INFO)
        .build();

    public StrayEntity(UUID uuid) {
        super(EntityType.STRAY, uuid);
    }
}
