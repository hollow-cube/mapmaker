package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.TadpoleMeta;

import java.util.UUID;

public class TadpoleEntity extends AbstractFishEntity<TadpoleMeta> {

    public static final MapEntityInfo<TadpoleEntity> INFO = MapEntityInfo.<TadpoleEntity>builder(AbstractFishEntity.INFO)
        .build();

    public TadpoleEntity(UUID uuid) {
        super(EntityType.TADPOLE, uuid);
    }
}
