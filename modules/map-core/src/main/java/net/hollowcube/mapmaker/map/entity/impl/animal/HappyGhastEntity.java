package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.HappyGhastMeta;

import java.util.UUID;

public class HappyGhastEntity extends AbstractAgeableEntity<HappyGhastMeta> {

    public static final MapEntityInfo<HappyGhastEntity> INFO = MapEntityInfo.<HappyGhastEntity>builder(AbstractAgeableEntity.INFO)
        .with("Harness", CommonMapEntityInfoTypes.DyeBodyArmor("_harness"))
        .build();

    public HappyGhastEntity(UUID uuid) {
        super(EntityType.HAPPY_GHAST, uuid);
    }
}
