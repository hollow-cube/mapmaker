package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.flying.PhantomMeta;

import java.util.UUID;

public class PhantomEntity extends AbstractMobEntity<PhantomMeta> {

    public static final MapEntityInfo<PhantomEntity> INFO = MapEntityInfo.<PhantomEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public PhantomEntity(UUID uuid) {
        super(EntityType.PHANTOM, uuid);
    }
}
