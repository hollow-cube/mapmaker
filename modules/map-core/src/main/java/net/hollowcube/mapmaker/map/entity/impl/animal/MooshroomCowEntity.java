package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.MooshroomMeta;

import java.util.UUID;

public class MooshroomCowEntity extends AbstractAgeableEntity<MooshroomMeta> {

    public static final MapEntityInfo<MooshroomCowEntity> INFO = MapEntityInfo.<MooshroomCowEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(MooshroomMeta.Variant.class, MooshroomMeta.Variant.RED, DataComponents.MOOSHROOM_VARIANT))
        .build();

    private static final String VARIANT_KEY = "Type";

    public MooshroomCowEntity(UUID uuid) {
        super(EntityType.MOOSHROOM, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.MOOSHROOM_VARIANT, NbtUtilV2.readStringEnum(tag.get(VARIANT_KEY), MooshroomMeta.Variant.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(VARIANT_KEY, NbtUtilV2.writeStringEnum(this.get(DataComponents.MOOSHROOM_VARIANT, MooshroomMeta.Variant.RED)));
    }
}
