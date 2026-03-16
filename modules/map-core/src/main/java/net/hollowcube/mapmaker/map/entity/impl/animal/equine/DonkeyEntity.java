package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.DonkeyMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DonkeyEntity extends AbstractChestedHorseEntity<DonkeyMeta> {

    public static final MapEntityInfo<@NotNull DonkeyEntity> INFO = MapEntityInfo.<DonkeyEntity>builder(AbstractChestedHorseEntity.INFO)
        .build();

    public DonkeyEntity(@NotNull UUID uuid) {
        super(EntityType.DONKEY, uuid);
    }
}
