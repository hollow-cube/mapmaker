package net.hollowcube.mapmaker.map.entity.impl.villager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.CreakingMeta;
import net.minestom.server.entity.metadata.villager.AbstractVillagerMeta;

import java.util.UUID;

public class AbstractVillagerEntity<M extends AbstractVillagerMeta> extends AbstractAgeableEntity<M> {

    public static final MapEntityInfo<AbstractVillagerEntity<? extends AbstractVillagerMeta>> INFO = MapEntityInfo.<AbstractVillagerEntity<? extends AbstractVillagerMeta>>builder(AbstractAgeableEntity.INFO)
        .build();

    public AbstractVillagerEntity(EntityType type, UUID uuid) {
        super(type, uuid);
    }
}
