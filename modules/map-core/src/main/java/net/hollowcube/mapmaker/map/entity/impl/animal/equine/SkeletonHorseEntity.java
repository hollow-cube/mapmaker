package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.SkeletonHorseMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SkeletonHorseEntity extends AbstractHorseEntity<SkeletonHorseMeta> {

    public static final MapEntityInfo<@NotNull SkeletonHorseEntity> INFO = MapEntityInfo.<SkeletonHorseEntity>builder(AbstractHorseEntity.ARMORED_INFO)
        .build();

    public SkeletonHorseEntity(@NotNull UUID uuid) {
        super(EntityType.SKELETON_HORSE, uuid);
    }
}
