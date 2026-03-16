package net.hollowcube.mapmaker.map.entity.impl.hostile.zombie;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.zombie.DrownedMeta;
import net.minestom.server.entity.metadata.monster.zombie.HuskMeta;

import java.util.UUID;

public class HuskEntity extends AbstractZombieEntity<HuskMeta> {

    public static final MapEntityInfo<HuskEntity> INFO = MapEntityInfo.<HuskEntity>builder(AbstractZombieEntity.INFO)
        .build();

    public HuskEntity(UUID uuid) {
        super(EntityType.HUSK, uuid);
    }
}
