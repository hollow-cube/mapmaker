package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.DonkeyMeta;

import java.util.UUID;

public class DonkeyEntity extends AbstractChestedHorseEntity<DonkeyMeta> {

    public static final MapEntityInfo<DonkeyEntity> INFO = MapEntityInfo.<DonkeyEntity>builder(AbstractChestedHorseEntity.INFO)
        .build();

    public DonkeyEntity(UUID uuid) {
        super(EntityType.DONKEY, uuid);
    }
}
