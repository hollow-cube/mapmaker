package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.EvokerMeta;

import java.util.UUID;

public class EvokerEntity extends AbstractSpellCastingIllagerEntity<EvokerMeta> {

    public static final MapEntityInfo<EvokerEntity> INFO = MapEntityInfo.<EvokerEntity>builder(AbstractSpellCastingIllagerEntity.INFO)
        .build();

    public EvokerEntity(UUID uuid) {
        super(EntityType.EVOKER, uuid);
    }
}
