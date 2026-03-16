package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.IllusionerMeta;

import java.util.UUID;

public class IllusionerEntity extends AbstractSpellCastingIllagerEntity<IllusionerMeta> {

    public static final MapEntityInfo<IllusionerEntity> INFO = MapEntityInfo.<IllusionerEntity>builder(AbstractIllagerEntity.INFO)
        .build();

    public IllusionerEntity(UUID uuid) {
        super(EntityType.ILLUSIONER, uuid);
    }
}
