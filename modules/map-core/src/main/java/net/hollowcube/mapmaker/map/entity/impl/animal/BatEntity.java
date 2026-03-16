package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.ambient.BatMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BatEntity extends AbstractMobEntity<BatMeta> {

    public static final MapEntityInfo<@NotNull BatEntity> INFO = MapEntityInfo.<BatEntity>builder(AbstractMobEntity.INFO)
        .with("Is Hanging", MapEntityInfoType.Bool(false, BatMeta::setHanging, BatMeta::isHanging))
        .build();

    private static final String BAT_FLAGS_KEY = "BatFlags";

    public BatEntity(@NotNull UUID uuid) {
        super(EntityType.BAT, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setHanging((tag.getInt(BAT_FLAGS_KEY, 0) & 1) != 0);
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putInt(BAT_FLAGS_KEY, this.getEntityMeta().isHanging() ? 1 : 0);
    }
}
