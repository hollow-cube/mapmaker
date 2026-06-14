package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.HorseMeta;

import java.util.UUID;

public class HorseEntity extends AbstractHorseEntity<HorseMeta> {

    public static final MapEntityInfo<HorseEntity> INFO = MapEntityInfo.<HorseEntity>builder(AbstractHorseEntity.ARMORED_INFO)
        .build();

    private static final String VARIANT_KEY = "Variant";

    public HorseEntity(UUID uuid) {
        super(EntityType.HORSE, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setVariant(HorseMeta.getVariantFromID(tag.getInt(VARIANT_KEY)));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putInt(VARIANT_KEY, HorseMeta.getVariantID(this.getEntityMeta().getVariant().getMarking(), this.getEntityMeta().getVariant().getColor()));
    }
}
