package net.hollowcube.mapmaker.map.entity.impl.hostile.zombie;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta;

import java.util.UUID;

public class ZombieEntity extends AbstractZombieEntity<ZombieMeta> {

    public static final MapEntityInfo<ZombieEntity> INFO = MapEntityInfo.<ZombieEntity>builder(AbstractZombieEntity.INFO)
        .build();

    public ZombieEntity(UUID uuid) {
        super(EntityType.ZOMBIE, uuid);
    }
}
