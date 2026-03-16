package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.EndermanMeta;
import net.minestom.server.entity.metadata.monster.WardenMeta;

import java.util.UUID;

public class WardenEntity extends AbstractMobEntity<WardenMeta> {

    public static final MapEntityInfo<WardenEntity> INFO = MapEntityInfo.<WardenEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public WardenEntity(UUID uuid) {
        super(EntityType.WARDEN, uuid);
    }
}
