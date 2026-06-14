package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.PillagerMeta;
import net.minestom.server.entity.metadata.monster.raider.VindicatorMeta;

import java.util.UUID;

public class VindicatorEntity extends AbstractIllagerEntity<VindicatorMeta> {

    public static final MapEntityInfo<VindicatorEntity> INFO = MapEntityInfo.<VindicatorEntity>builder(AbstractIllagerEntity.INFO)
        .build();

    public VindicatorEntity(UUID uuid) {
        super(EntityType.VINDICATOR, uuid);
    }
}
