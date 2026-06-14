package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.MuleMeta;

import java.util.UUID;

public class MuleEntity extends AbstractChestedHorseEntity<MuleMeta> {

    public static final MapEntityInfo<MuleEntity> INFO = MapEntityInfo.<MuleEntity>builder(AbstractChestedHorseEntity.INFO)
        .build();

    public MuleEntity(UUID uuid) {
        super(EntityType.MULE, uuid);
    }
}
