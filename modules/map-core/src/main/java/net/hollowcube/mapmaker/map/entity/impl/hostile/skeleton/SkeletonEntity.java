package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.ParchedMeta;
import net.minestom.server.entity.metadata.monster.skeleton.SkeletonMeta;

import java.util.UUID;

public class SkeletonEntity extends AbstractSkeletonEntity<SkeletonMeta> {

    public static final MapEntityInfo<SkeletonEntity> INFO = MapEntityInfo.<SkeletonEntity>builder(AbstractSkeletonEntity.INFO)
        .build();

    public SkeletonEntity(UUID uuid) {
        super(EntityType.SKELETON, uuid);
    }
}
