package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.PigMeta;
import net.minestom.server.entity.metadata.animal.PigVariant;
import net.minestom.server.registry.Registries;

import java.util.UUID;

public class PigEntity extends AbstractAgeableEntity<PigMeta> {

    public static final MapEntityInfo<PigEntity> INFO = MapEntityInfo.<PigEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::pigVariant, PigVariant.TEMPERATE, DataComponents.PIG_VARIANT))
        .build();

    private static final String VARIANT_KEY = "variant";

    public PigEntity(UUID uuid) {
        super(EntityType.PIG, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.PIG_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::pigVariant, PigVariant.TEMPERATE));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.PIG_VARIANT, PigVariant.TEMPERATE).name());
    }
}
