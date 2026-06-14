package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.GlowSquidMeta;

import java.util.UUID;

public class GlowSquidEntity extends AbstractAgeableEntity<GlowSquidMeta> {

    public static final MapEntityInfo<GlowSquidEntity> INFO = MapEntityInfo.<GlowSquidEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public GlowSquidEntity(UUID uuid) {
        super(EntityType.GLOW_SQUID, uuid);
    }
}
