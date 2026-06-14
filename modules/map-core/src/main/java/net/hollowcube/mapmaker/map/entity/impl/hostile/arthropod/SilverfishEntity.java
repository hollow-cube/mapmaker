package net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.SilverfishMeta;

import java.util.UUID;

public class SilverfishEntity extends AbstractMobEntity<SilverfishMeta> {

    public static final MapEntityInfo<SilverfishEntity> INFO = MapEntityInfo.<SilverfishEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public SilverfishEntity(UUID uuid) {
        super(EntityType.SILVERFISH, uuid);
    }
}
