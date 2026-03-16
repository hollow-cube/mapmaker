package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.RabbitMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RabbitEntity extends AbstractAgeableEntity<RabbitMeta> {

    public static final MapEntityInfo<@NotNull RabbitEntity> INFO = MapEntityInfo.<RabbitEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(RabbitMeta.Variant.class, RabbitMeta.Variant.BROWN, DataComponents.RABBIT_VARIANT))
        .build();

    private static final String TYPE_KEY = "RabbitType";

    public RabbitEntity(@NotNull UUID uuid) {
        super(EntityType.RABBIT, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.RABBIT_VARIANT, NbtUtilV2.readIntEnum(tag.get(TYPE_KEY), RabbitMeta.Variant.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(TYPE_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.RABBIT_VARIANT, RabbitMeta.Variant.BROWN)));
    }
}
