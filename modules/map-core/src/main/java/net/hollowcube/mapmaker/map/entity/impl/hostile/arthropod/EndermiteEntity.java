package net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.EndermiteMeta;
import net.minestom.server.entity.metadata.monster.SilverfishMeta;

import java.util.UUID;

public class EndermiteEntity extends AbstractMobEntity<EndermiteMeta> {

    public static final MapEntityInfo<EndermiteEntity> INFO = MapEntityInfo.<EndermiteEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public EndermiteEntity(UUID uuid) {
        super(EntityType.ENDERMITE, uuid);
    }
}
