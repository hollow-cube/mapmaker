package net.hollowcube.mapmaker.map.entity.impl.hostile.zombie;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.zombie.DrownedMeta;
import net.minestom.server.entity.metadata.monster.zombie.ZombifiedPiglinMeta;

import java.util.UUID;

public class ZombifiedPiglinEntity extends AbstractZombieEntity<ZombifiedPiglinMeta> {

    public static final MapEntityInfo<ZombifiedPiglinEntity> INFO = MapEntityInfo.<ZombifiedPiglinEntity>builder(AbstractZombieEntity.INFO)
        .build();

    public ZombifiedPiglinEntity(UUID uuid) {
        super(EntityType.ZOMBIFIED_PIGLIN, uuid);
    }
}
