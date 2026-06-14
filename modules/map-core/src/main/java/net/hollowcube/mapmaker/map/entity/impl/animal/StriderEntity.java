package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.StriderMeta;

import java.util.UUID;

public class StriderEntity extends AbstractAgeableEntity<StriderMeta> {

    public static final MapEntityInfo<StriderEntity> INFO = MapEntityInfo.<StriderEntity>builder(AbstractAgeableEntity.INFO)
        .with("Saddled", CommonMapEntityInfoTypes.Saddle())
        .build();

    public StriderEntity(UUID uuid) {
        super(EntityType.STRIDER, uuid);
    }
}
