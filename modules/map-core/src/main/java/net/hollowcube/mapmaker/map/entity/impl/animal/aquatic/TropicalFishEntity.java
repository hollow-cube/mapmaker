package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.TropicalFishMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TropicalFishEntity extends AbstractFishEntity<TropicalFishMeta> {

    public static final MapEntityInfo<@NotNull TropicalFishEntity> INFO = MapEntityInfo.<TropicalFishEntity>builder(AbstractFishEntity.INFO)
        .with("Pattern Color", MapEntityInfoType.Enum(DyeColor.class, DyeColor.WHITE, DataComponents.TROPICAL_FISH_PATTERN_COLOR))
        .with("Base Color", MapEntityInfoType.Enum(DyeColor.class, DyeColor.WHITE, DataComponents.TROPICAL_FISH_BASE_COLOR))
        .with("Pattern", MapEntityInfoType.Enum(TropicalFishMeta.Pattern.class, TropicalFishMeta.Pattern.KOB, DataComponents.TROPICAL_FISH_PATTERN))
        .build();

    private static final String VARIANT_KEY = "Variant";

    public TropicalFishEntity(@NotNull UUID uuid) {
        super(EntityType.TROPICAL_FISH, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        var variant = TropicalFishMeta.Variant.fromPackedId(tag.getInt(VARIANT_KEY, 0));

        // Vanilla
        this.set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, variant.patternColor());
        this.set(DataComponents.TROPICAL_FISH_BASE_COLOR, variant.baseColor());
        this.set(DataComponents.TROPICAL_FISH_PATTERN, variant.pattern());
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putInt(VARIANT_KEY, new TropicalFishMeta.Variant(
            this.get(DataComponents.TROPICAL_FISH_PATTERN, TropicalFishMeta.Pattern.KOB),
            this.get(DataComponents.TROPICAL_FISH_PATTERN_COLOR, DyeColor.WHITE),
            this.get(DataComponents.TROPICAL_FISH_BASE_COLOR, DyeColor.WHITE)
        ).packedId());
    }
}
