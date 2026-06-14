package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.StrayMeta;
import net.minestom.server.entity.metadata.monster.skeleton.WitherSkeletonMeta;

import java.util.UUID;

public class WitherSkeletonEntity extends AbstractSkeletonEntity<WitherSkeletonMeta> {

    public static final MapEntityInfo<WitherSkeletonEntity> INFO = MapEntityInfo.<WitherSkeletonEntity>builder(AbstractSkeletonEntity.INFO)
        .build();

    public WitherSkeletonEntity(UUID uuid) {
        super(EntityType.WITHER_SKELETON, uuid);
    }
}
