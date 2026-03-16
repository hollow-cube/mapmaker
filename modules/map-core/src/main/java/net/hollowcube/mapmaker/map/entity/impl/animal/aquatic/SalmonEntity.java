package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.SalmonMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SalmonEntity extends AbstractFishEntity<SalmonMeta> {

    public static final MapEntityInfo<@NotNull SalmonEntity> INFO = MapEntityInfo.<SalmonEntity>builder(AbstractFishEntity.INFO)
        .with("Size", MapEntityInfoType.Enum(SalmonMeta.Size.class, SalmonMeta.Size.MEDIUM, DataComponents.SALMON_SIZE))
        .build();

    private static final String TYPE_KEY = "type";

    public SalmonEntity(@NotNull UUID uuid) {
        super(EntityType.SALMON, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.SALMON_SIZE, NbtUtilV2.readStringEnum(tag.get(TYPE_KEY), SalmonMeta.Size.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(TYPE_KEY, NbtUtilV2.writeStringEnum(this.get(DataComponents.SALMON_SIZE, SalmonMeta.Size.MEDIUM)));
    }
}
