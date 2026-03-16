package net.hollowcube.mapmaker.map.entity.impl.base;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.ChestedHorseMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractChestedHorseEntity<M extends ChestedHorseMeta> extends AbstractHorseEntity<M> {

    public static final MapEntityInfo<@NotNull AbstractChestedHorseEntity<? extends ChestedHorseMeta>> INFO = MapEntityInfo.<AbstractChestedHorseEntity<? extends ChestedHorseMeta>>builder(AbstractHorseEntity.INFO)
        .with("Has Chest", MapEntityInfoType.Bool(false, ChestedHorseMeta::setHasChest, ChestedHorseMeta::isHasChest))
        .build();

    private static final String CHESTED_KEY = "ChestedHorse";

    protected AbstractChestedHorseEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractChestedHorseEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setHasChest(tag.getBoolean(CHESTED_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(CHESTED_KEY, this.getEntityMeta().isHasChest());
    }

}
