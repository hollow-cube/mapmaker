package net.hollowcube.mapmaker.map.entity.impl.animal.golem;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.golem.SnowGolemMeta;

import java.util.UUID;

public class SnowGolemEntity extends AbstractMobEntity<SnowGolemMeta> {

    public static final MapEntityInfo<SnowGolemEntity> INFO = MapEntityInfo.<SnowGolemEntity>builder(AbstractLivingEntity.INFO)
        .with("Pumpkin", MapEntityInfoType.Bool(false, SnowGolemMeta::setHasPumpkinHat, SnowGolemMeta::isHasPumpkinHat))
        .build();

    private static final String PUMPKIN_KEY = "Pumpkin";

    public SnowGolemEntity(UUID uuid) {
        super(EntityType.SNOW_GOLEM, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setHasPumpkinHat(tag.getBoolean(PUMPKIN_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(PUMPKIN_KEY, this.getEntityMeta().isHasPumpkinHat());
    }
}
