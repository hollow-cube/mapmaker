package net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.SpiderMeta;

import java.util.UUID;

public class SpiderEntity extends AbstractMobEntity<SpiderMeta> {

    public static final MapEntityInfo<SpiderEntity> INFO = MapEntityInfo.<SpiderEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public SpiderEntity(UUID uuid) {
        super(EntityType.SPIDER, uuid);
    }
}
