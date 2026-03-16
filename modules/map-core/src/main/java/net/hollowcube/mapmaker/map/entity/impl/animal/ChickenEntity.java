package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.ChickenMeta;
import net.minestom.server.entity.metadata.animal.ChickenVariant;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChickenEntity extends AbstractAgeableEntity<ChickenMeta> {

    public static final MapEntityInfo<@NotNull ChickenEntity> INFO = MapEntityInfo.<ChickenEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::chickenVariant, ChickenVariant.TEMPERATE, DataComponents.CHICKEN_VARIANT))
        .build();

    private static final String VARIANT_KEY = "variant";

    public ChickenEntity(@NotNull UUID uuid) {
        super(EntityType.CHICKEN, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.CHICKEN_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::chickenVariant, ChickenVariant.TEMPERATE));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.CHICKEN_VARIANT, ChickenVariant.TEMPERATE).name());
    }
}
