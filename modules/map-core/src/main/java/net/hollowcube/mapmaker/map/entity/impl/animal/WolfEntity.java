package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractTameableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.tameable.WolfMeta;
import net.minestom.server.entity.metadata.animal.tameable.WolfVariant;
import net.minestom.server.registry.Registries;

import java.util.UUID;

public class WolfEntity extends AbstractTameableEntity<WolfMeta> {

    public static final MapEntityInfo<WolfEntity> INFO = MapEntityInfo.<WolfEntity>builder(AbstractTameableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::wolfVariant, WolfVariant.PALE, DataComponents.WOLF_VARIANT))
        .with("Collar", MapEntityInfoType.Enum(DyeColor.class, DyeColor.RED, DataComponents.WOLF_COLLAR))
        .build();

    private static final String VARIANT_KEY = "variant";
    private static final String COLLAR_COLOR_KEY = "CollarColor";

    public WolfEntity(UUID uuid) {
        super(EntityType.WOLF, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.WOLF_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::wolfVariant, WolfVariant.PALE));
        this.set(DataComponents.WOLF_COLLAR, NbtUtilV2.readIntEnum(tag.get(COLLAR_COLOR_KEY), DyeColor.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.WOLF_VARIANT, WolfVariant.PALE).name());
        tag.put(COLLAR_COLOR_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.WOLF_COLLAR, DyeColor.RED)));
    }
}
