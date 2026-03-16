package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.PolarBearMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PolarBearEntity extends AbstractAgeableEntity<PolarBearMeta> {

    public static final MapEntityInfo<@NotNull PolarBearEntity> INFO = MapEntityInfo.<PolarBearEntity>builder(AbstractAgeableEntity.INFO)
        .with("Standing", MapEntityInfoType.Bool(false, PolarBearMeta::setStandingUp, PolarBearMeta::isStandingUp))
        .build();

    private static final String STANDING_KEY = "mapmaker:standing";

    public PolarBearEntity(@NotNull UUID uuid) {
        super(EntityType.POLAR_BEAR, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setStandingUp(tag.getBoolean(STANDING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(STANDING_KEY, this.getEntityMeta().isStandingUp());
    }
}
