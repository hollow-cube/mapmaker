package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.CowMeta;
import net.minestom.server.entity.metadata.animal.CowVariant;
import net.minestom.server.registry.Registries;

import java.util.UUID;

public class CowEntity extends AbstractAgeableEntity<CowMeta> {

    public static final MapEntityInfo<CowEntity> INFO = MapEntityInfo.<CowEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::cowVariant, CowVariant.TEMPERATE, DataComponents.COW_VARIANT))
        .build();

    private static final String VARIANT_KEY = "variant";

    public CowEntity(UUID uuid) {
        super(EntityType.COW, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.COW_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::cowVariant, CowVariant.TEMPERATE));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.COW_VARIANT, CowVariant.TEMPERATE).name());
    }
}
