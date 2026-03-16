package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.ElderGuardianMeta;

import java.util.UUID;

public class ElderGuardianEntity extends AbstractMobEntity<ElderGuardianMeta> {

    public static final MapEntityInfo<ElderGuardianEntity> INFO = MapEntityInfo.<ElderGuardianEntity>builder(AbstractLivingEntity.INFO)
        .with("Spikes Retracted", MapEntityInfoType.Bool(false, ElderGuardianMeta::setRetractingSpikes, ElderGuardianMeta::isRetractingSpikes))
        .build();

    private static final String IS_SPIKES_RETRACTED = "mapmaker:is_spikes_retracted";

    public ElderGuardianEntity(UUID uuid) {
        super(EntityType.ELDER_GUARDIAN, uuid);
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
