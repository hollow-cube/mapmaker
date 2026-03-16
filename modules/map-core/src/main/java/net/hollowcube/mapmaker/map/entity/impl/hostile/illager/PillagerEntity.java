package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.PillagerMeta;

import java.util.UUID;

public class PillagerEntity extends AbstractIllagerEntity<PillagerMeta> {

    public static final MapEntityInfo<PillagerEntity> INFO = MapEntityInfo.<PillagerEntity>builder(AbstractIllagerEntity.INFO)
        .build();

    public PillagerEntity(UUID uuid) {
        super(EntityType.PILLAGER, uuid);
    }
}
