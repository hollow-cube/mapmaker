package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.TurtleMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TurtleEntity extends AbstractAgeableEntity<TurtleMeta> {

    public static final MapEntityInfo<@NotNull TurtleEntity> INFO = MapEntityInfo.<TurtleEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public TurtleEntity(@NotNull UUID uuid) {
        super(EntityType.TURTLE, uuid);
    }
}
