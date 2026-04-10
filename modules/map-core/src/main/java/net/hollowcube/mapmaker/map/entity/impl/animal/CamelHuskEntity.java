package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.CamelMeta;

import java.util.UUID;

public class CamelHuskEntity extends AbstractHorseEntity<CamelMeta> {

    public static final MapEntityInfo<CamelHuskEntity> INFO = MapEntityInfo.<CamelHuskEntity>builder(AbstractHorseEntity.INFO)
        .with("Is Sitting", MapEntityInfoType.Bool(
            false,
            (meta, sitting) -> meta.setLastPoseChangeTick(sitting ? -1 : 0),
            (meta) -> meta.getLastPoseChangeTick() < 0
        ))
        .build();

    private static final String LAST_POSE_TICK_KEY = "LastPoseTick";

    public CamelHuskEntity(UUID uuid) {
        super(EntityType.CAMEL_HUSK, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setLastPoseChangeTick(tag.getLong(LAST_POSE_TICK_KEY, 0));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putLong(LAST_POSE_TICK_KEY, this.getEntityMeta().getLastPoseChangeTick());
    }
}
