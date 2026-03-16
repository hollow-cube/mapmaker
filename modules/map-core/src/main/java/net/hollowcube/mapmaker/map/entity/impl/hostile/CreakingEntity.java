package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.CreakingMeta;

import java.util.UUID;

public class CreakingEntity extends AbstractMobEntity<CreakingMeta> {

    public static final MapEntityInfo<CreakingEntity> INFO = MapEntityInfo.<CreakingEntity>builder(AbstractLivingEntity.INFO)
        .with("Glowing Eyes", MapEntityInfoType.Bool(false, CreakingMeta::setActive, CreakingMeta::isActive))
        .build();

    private static final String IS_ACTIVE_KEY = "mapmaker:is_active";

    public CreakingEntity(UUID uuid) {
        super(EntityType.CREAKING, uuid);
    }
    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Mapmaker
        this.getEntityMeta().setActive(tag.getBoolean(IS_ACTIVE_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Mapmaker
        tag.putBoolean(IS_ACTIVE_KEY, this.getEntityMeta().isActive());
    }
}
