package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.GlowSquidMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GlowSquidEntity extends AbstractAgeableEntity<GlowSquidMeta> {

    public static final MapEntityInfo<@NotNull GlowSquidEntity> INFO = MapEntityInfo.<GlowSquidEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public GlowSquidEntity(@NotNull UUID uuid) {
        super(EntityType.GLOW_SQUID, uuid);
    }
}
