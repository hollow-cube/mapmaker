package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractTameableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.tameable.CatMeta;
import net.minestom.server.entity.metadata.animal.tameable.CatVariant;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CatEntity extends AbstractTameableEntity<CatMeta> {

    public static final MapEntityInfo<@NotNull CatEntity> INFO = MapEntityInfo.<CatEntity>builder(AbstractTameableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::catVariant, CatVariant.BLACK, DataComponents.CAT_VARIANT))
        .with("Collar", MapEntityInfoType.Enum(DyeColor.class, DyeColor.RED, DataComponents.CAT_COLLAR))
        .with("Is Lying", MapEntityInfoType.Bool(false, CatMeta::setLying, CatMeta::isLying))
        .build();

    private static final String VARIANT_KEY = "variant";
    private static final String COLLAR_COLOR_KEY = "CollarColor";
    private static final String LYING_KEY = "mapmaker:lying";

    public CatEntity(@NotNull UUID uuid) {
        super(EntityType.CAT, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.CAT_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::catVariant, CatVariant.BLACK));
        this.set(DataComponents.CAT_COLLAR, NbtUtilV2.readIntEnum(tag.get(COLLAR_COLOR_KEY), DyeColor.class));

        // Mapmaker
        this.getEntityMeta().setLying(tag.getBoolean(LYING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.CAT_VARIANT, CatVariant.BLACK).name());
        tag.putInt(COLLAR_COLOR_KEY, this.get(DataComponents.CAT_COLLAR, DyeColor.RED).ordinal());

        // Mapmaker
        tag.putBoolean(LYING_KEY, this.getEntityMeta().isLying());
    }
}
