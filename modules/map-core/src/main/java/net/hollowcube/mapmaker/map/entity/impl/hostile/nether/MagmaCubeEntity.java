package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.MagmaCubeMeta;

import java.util.UUID;

public class MagmaCubeEntity extends AbstractMobEntity<MagmaCubeMeta> {

    public static final MapEntityInfo<MagmaCubeEntity> INFO = MapEntityInfo.<MagmaCubeEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public MagmaCubeEntity(UUID uuid) {
        super(EntityType.MAGMA_CUBE, uuid);
    }
}
