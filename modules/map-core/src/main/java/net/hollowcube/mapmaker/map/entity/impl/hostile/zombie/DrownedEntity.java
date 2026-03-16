package net.hollowcube.mapmaker.map.entity.impl.hostile.zombie;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.zombie.DrownedMeta;
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta;

import java.util.UUID;

public class DrownedEntity extends AbstractZombieEntity<DrownedMeta> {

    public static final MapEntityInfo<DrownedEntity> INFO = MapEntityInfo.<DrownedEntity>builder(AbstractZombieEntity.INFO)
        .build();

    public DrownedEntity(UUID uuid) {
        super(EntityType.DROWNED, uuid);
    }
}
