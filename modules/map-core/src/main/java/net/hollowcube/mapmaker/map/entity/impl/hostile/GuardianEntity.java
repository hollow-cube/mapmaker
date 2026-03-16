package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.GuardianMeta;

import java.util.UUID;

public class GuardianEntity extends AbstractMobEntity<GuardianMeta> {

    public static final MapEntityInfo<GuardianEntity> INFO = MapEntityInfo.<GuardianEntity>builder(AbstractLivingEntity.INFO)
        .with("Spikes Retracted", MapEntityInfoType.Bool(false, GuardianMeta::setRetractingSpikes, GuardianMeta::isRetractingSpikes))
        .build();

    private static final String IS_SPIKES_RETRACTED = "mapmaker:is_spikes_retracted";

    public GuardianEntity(UUID uuid) {
        super(EntityType.GUARDIAN, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Mapmaker
        this.getEntityMeta().setRetractingSpikes(tag.getBoolean(IS_SPIKES_RETRACTED, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Mapmaker
        tag.putBoolean(IS_SPIKES_RETRACTED, this.getEntityMeta().isRetractingSpikes());
    }
}
