package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.BlazeMeta;

import java.util.UUID;

public class BlazeEntity extends AbstractMobEntity<BlazeMeta> {

    public static final MapEntityInfo<BlazeEntity> INFO = MapEntityInfo.<BlazeEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public BlazeEntity(UUID uuid) {
        super(EntityType.BLAZE, uuid);
    }
}
