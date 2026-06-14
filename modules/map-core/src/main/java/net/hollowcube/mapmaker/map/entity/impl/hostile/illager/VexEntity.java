package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.VexMeta;
import net.minestom.server.entity.metadata.monster.raider.VindicatorMeta;

import java.util.UUID;

public class VexEntity extends AbstractMobEntity<VexMeta> {

    public static final MapEntityInfo<VexEntity> INFO = MapEntityInfo.<VexEntity>builder(AbstractLivingEntity.INFO)
        .with("Attacking", MapEntityInfoType.Bool(false, VexMeta::setAttacking, VexMeta::isAttacking))
        .build();

    public VexEntity(UUID uuid) {
        super(EntityType.VEX, uuid);
    }
}
