package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.SkeletonHorseMeta;

import java.util.UUID;

public class ZombieHorseEntity extends AbstractHorseEntity<SkeletonHorseMeta> {

    public static final MapEntityInfo<ZombieHorseEntity> INFO = MapEntityInfo.<ZombieHorseEntity>builder(AbstractHorseEntity.ARMORED_INFO)
        .build();

    public ZombieHorseEntity(UUID uuid) {
        super(EntityType.ZOMBIE_HORSE, uuid);
    }
}
