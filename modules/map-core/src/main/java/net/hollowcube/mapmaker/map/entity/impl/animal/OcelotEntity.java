package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.OcelotMeta;

import java.util.UUID;

public class OcelotEntity extends AbstractAgeableEntity<OcelotMeta> {

    public static final MapEntityInfo<OcelotEntity> INFO = MapEntityInfo.<OcelotEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public OcelotEntity(UUID uuid) {
        super(EntityType.OCELOT, uuid);
    }
}
