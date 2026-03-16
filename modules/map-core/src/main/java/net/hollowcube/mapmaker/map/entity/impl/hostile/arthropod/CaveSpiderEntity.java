package net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.CaveSpiderMeta;

import java.util.UUID;

public class CaveSpiderEntity extends AbstractMobEntity<CaveSpiderMeta> {

    public static final MapEntityInfo<CaveSpiderEntity> INFO = MapEntityInfo.<CaveSpiderEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public CaveSpiderEntity(UUID uuid) {
        super(EntityType.CAVE_SPIDER, uuid);
    }
}
