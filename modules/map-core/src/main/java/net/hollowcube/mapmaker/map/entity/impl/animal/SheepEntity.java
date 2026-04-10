package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.SheepMeta;

import java.util.UUID;

public class SheepEntity extends AbstractAgeableEntity<SheepMeta> {

    public static final MapEntityInfo<SheepEntity> INFO = MapEntityInfo.<SheepEntity>builder(AbstractAgeableEntity.INFO)
        .with("Color", MapEntityInfoType.Enum(DyeColor.class, DyeColor.WHITE, DataComponents.SHEEP_COLOR))
        .with("Sheared", MapEntityInfoType.Bool(false, SheepMeta::setSheared, SheepMeta::isSheared))
        .build();

    private static final String COLOR_KEY = "Color";
    private static final String SHEARED_KEY = "Sheared";

    public SheepEntity(UUID uuid) {
        super(EntityType.SHEEP, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.SHEEP_COLOR, NbtUtilV2.readIntEnum(tag.get(COLOR_KEY), DyeColor.class));
        this.getEntityMeta().setSheared(tag.getBoolean(SHEARED_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(COLOR_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.SHEEP_COLOR, DyeColor.WHITE)));
        tag.putBoolean(SHEARED_KEY, this.getEntityMeta().isSheared());
    }
}
