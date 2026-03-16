package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.AbstractSkeletonMeta;

import java.util.UUID;

public class AbstractSkeletonEntity<M extends AbstractSkeletonMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<AbstractSkeletonEntity<? extends AbstractSkeletonMeta>> INFO = MapEntityInfo.<AbstractSkeletonEntity<? extends AbstractSkeletonMeta>>builder(AbstractLivingEntity.INFO)
        .build();

    protected AbstractSkeletonEntity(EntityType type, UUID uuid) {
        super(type, uuid);
    }
}
