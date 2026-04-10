package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractTameableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.tameable.ParrotMeta;

import java.util.UUID;

public class ParrotEntity extends AbstractTameableEntity<ParrotMeta> {

    public static final MapEntityInfo<ParrotEntity> INFO = MapEntityInfo.<ParrotEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(ParrotMeta.Color.class, ParrotMeta.Color.RED_BLUE, DataComponents.PARROT_VARIANT))
        .build();

    private static final String VARIANT_KEY = "Variant";

    public ParrotEntity(UUID uuid) {
        super(EntityType.PARROT, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.PARROT_VARIANT, NbtUtilV2.readIntEnum(tag.get(VARIANT_KEY), ParrotMeta.Color.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(VARIANT_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.PARROT_VARIANT, ParrotMeta.Color.RED_BLUE)));
    }

}
