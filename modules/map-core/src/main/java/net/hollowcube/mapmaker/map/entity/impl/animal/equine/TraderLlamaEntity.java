package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.LlamaMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TraderLlamaEntity extends AbstractChestedHorseEntity<LlamaMeta> {

    public static final MapEntityInfo<@NotNull TraderLlamaEntity> INFO = MapEntityInfo.<TraderLlamaEntity>builder(AbstractChestedHorseEntity.INFO)
        .build();

    public TraderLlamaEntity(@NotNull UUID uuid) {
        super(EntityType.TRADER_LLAMA, uuid);
    }
}
