package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.CodMeta;

import java.util.UUID;

public class CodEntity extends AbstractFishEntity<CodMeta> {

    public static final MapEntityInfo<CodEntity> INFO = MapEntityInfo.<CodEntity>builder(AbstractFishEntity.INFO)
        .build();

    public CodEntity(UUID uuid) {
        super(EntityType.COD, uuid);
    }
}
