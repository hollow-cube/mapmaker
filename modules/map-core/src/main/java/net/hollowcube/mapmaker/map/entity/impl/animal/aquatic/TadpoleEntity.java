package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.TadpoleMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TadpoleEntity extends AbstractFishEntity<TadpoleMeta> {

    public static final MapEntityInfo<@NotNull TadpoleEntity> INFO = MapEntityInfo.<TadpoleEntity>builder(AbstractFishEntity.INFO)
        .build();

    public TadpoleEntity(@NotNull UUID uuid) {
        super(EntityType.TADPOLE, uuid);
    }
}
