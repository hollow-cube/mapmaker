package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.FrogMeta;
import net.minestom.server.entity.metadata.animal.FrogVariant;
import net.minestom.server.registry.Registries;

import java.util.UUID;

public class FrogEntity extends AbstractAgeableEntity<FrogMeta> {

    public static final MapEntityInfo<FrogEntity> INFO = MapEntityInfo.<FrogEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::frogVariant, FrogVariant.TEMPERATE, DataComponents.FROG_VARIANT))
        .build();

    private static final String VARIANT_KEY = "variant";

    public FrogEntity(UUID uuid) {
        super(EntityType.FROG, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.FROG_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::frogVariant, FrogVariant.TEMPERATE));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.FROG_VARIANT, FrogVariant.TEMPERATE).name());
    }
}
