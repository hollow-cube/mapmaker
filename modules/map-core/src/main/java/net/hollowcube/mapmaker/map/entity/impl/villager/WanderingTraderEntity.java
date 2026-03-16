package net.hollowcube.mapmaker.map.entity.impl.villager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.villager.VillagerMeta;

import java.util.UUID;

public class WanderingTraderEntity extends AbstractVillagerEntity<VillagerMeta> {

    public static final MapEntityInfo<WanderingTraderEntity> INFO = MapEntityInfo.<WanderingTraderEntity>builder(AbstractAgeableEntity.INFO)
        .build();

    public WanderingTraderEntity(UUID uuid) {
        super(EntityType.WANDERING_TRADER, uuid);
    }
}
