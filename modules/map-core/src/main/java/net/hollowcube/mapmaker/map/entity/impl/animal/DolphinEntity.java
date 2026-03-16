package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.DolphinMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DolphinEntity extends AbstractAgeableEntity<DolphinMeta> {

    public static final MapEntityInfo<@NotNull DolphinEntity> INFO = MapEntityInfo.<DolphinEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public DolphinEntity(@NotNull UUID uuid) {
        super(EntityType.DOLPHIN, uuid);
    }
}
