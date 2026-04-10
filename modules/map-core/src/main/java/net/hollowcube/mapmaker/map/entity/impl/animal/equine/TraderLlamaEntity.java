package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.LlamaMeta;

import java.util.UUID;

public class TraderLlamaEntity extends AbstractChestedHorseEntity<LlamaMeta> {

    public static final MapEntityInfo<TraderLlamaEntity> INFO = MapEntityInfo.<TraderLlamaEntity>builder(AbstractChestedHorseEntity.INFO)
        .build();

    public TraderLlamaEntity(UUID uuid) {
        super(EntityType.TRADER_LLAMA, uuid);
    }
}
