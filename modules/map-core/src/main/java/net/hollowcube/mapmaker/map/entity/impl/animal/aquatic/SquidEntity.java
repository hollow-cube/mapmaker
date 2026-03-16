package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.SquidMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SquidEntity extends AbstractAgeableEntity<SquidMeta> {

    public static final MapEntityInfo<@NotNull SquidEntity> INFO = MapEntityInfo.<SquidEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public SquidEntity(@NotNull UUID uuid) {
        super(EntityType.SQUID, uuid);
    }
}
