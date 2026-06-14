package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.flying.PhantomMeta;
import net.minestom.server.entity.metadata.other.SlimeMeta;

import java.util.UUID;

public class SlimeEntity extends AbstractMobEntity<SlimeMeta> {

    public static final MapEntityInfo<SlimeEntity> INFO = MapEntityInfo.<SlimeEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public SlimeEntity(UUID uuid) {
        super(EntityType.SLIME, uuid);
    }
}
