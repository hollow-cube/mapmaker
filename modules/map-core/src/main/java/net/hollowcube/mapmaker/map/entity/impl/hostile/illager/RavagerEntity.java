package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.RavagerMeta;
import net.minestom.server.entity.metadata.monster.raider.VindicatorMeta;

import java.util.UUID;

public class RavagerEntity extends AbstractIllagerEntity<RavagerMeta> {

    public static final MapEntityInfo<RavagerEntity> INFO = MapEntityInfo.<RavagerEntity>builder(AbstractIllagerEntity.INFO)
        .build();

    public RavagerEntity(UUID uuid) {
        super(EntityType.RAVAGER, uuid);
    }
}
