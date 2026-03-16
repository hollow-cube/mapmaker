package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.BreezeMeta;
import net.minestom.server.entity.metadata.monster.skeleton.WitherSkeletonMeta;

import java.util.UUID;

public class BreezeEntity extends AbstractMobEntity<BreezeMeta> {

    public static final MapEntityInfo<BreezeEntity> INFO = MapEntityInfo.<BreezeEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public BreezeEntity(UUID uuid) {
        super(EntityType.BREEZE, uuid);
    }
}
