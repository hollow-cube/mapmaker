package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.SnifferMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SnifferEntity extends AbstractAgeableEntity<SnifferMeta> {

    public static final MapEntityInfo<@NotNull SnifferEntity> INFO = MapEntityInfo.<SnifferEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public SnifferEntity(@NotNull UUID uuid) {
        super(EntityType.SNIFFER, uuid);
    }
}
