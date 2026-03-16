package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.PiglinMeta;

import java.util.UUID;

public class PiglinBruteEntity extends AbstractPiglinEntity<PiglinMeta> {

    public static final MapEntityInfo<PiglinBruteEntity> INFO = MapEntityInfo.<PiglinBruteEntity>builder(AbstractPiglinEntity.INFO)
        .build();

    public PiglinBruteEntity(UUID uuid) {
        super(EntityType.PIGLIN_BRUTE, uuid);
    }
}
