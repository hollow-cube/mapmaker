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

        // Vanilla (variant = color | marking << 8)
        var variantId = tag.getInt(VARIANT_KEY);
        this.getEntityMeta().setVariantAndMarking(
            HorseMeta.Variant.values()[variantId & 0xFF],
            HorseMeta.Marking.values()[variantId >> 8]);
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla (variant = color | marking << 8)
        var meta = this.getEntityMeta();
        tag.putInt(VARIANT_KEY, (meta.getMarking().ordinal() << 8) + meta.getVariant().ordinal());
    }
}
