package net.hollowcube.mapmaker.map.entity.impl.animal.golem;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.golem.IronGolemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IronGolemEntity extends AbstractMobEntity<IronGolemMeta> {

    public static final MapEntityInfo<@NotNull IronGolemEntity> INFO = MapEntityInfo.<IronGolemEntity>builder(AbstractLivingEntity.INFO)
        .with("Health", MapEntityInfoType.Float64(
            100.0,
            1.0,
            100.0,
            1.0,
            (meta, health) -> meta.setHealth(health.floatValue()),
            meta -> (double) meta.getHealth()
        ))
        .build();

    public IronGolemEntity(@NotNull UUID uuid) {
        super(EntityType.IRON_GOLEM, uuid);

        this.getEntityMeta().setHealth(100.0f);
    }
}
