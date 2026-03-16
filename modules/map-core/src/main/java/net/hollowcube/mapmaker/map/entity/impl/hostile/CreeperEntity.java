package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.CreakingMeta;
import net.minestom.server.entity.metadata.monster.CreeperMeta;

import java.util.UUID;

public class CreeperEntity extends AbstractMobEntity<CreeperMeta> {

    public static final MapEntityInfo<CreeperEntity> INFO = MapEntityInfo.<CreeperEntity>builder(AbstractLivingEntity.INFO)
        .with("Powered", MapEntityInfoType.Bool(false, CreeperMeta::setCharged, CreeperMeta::isCharged))
        .with("Ignited", MapEntityInfoType.Bool(false, CreeperMeta::setIgnited, CreeperMeta::isIgnited))
        // swelling
        .build();

    private static final String POWERED_KEY = "powered";
    private static final String IGNITED_KEY = "ignited";

    public CreeperEntity(UUID uuid) {
        super(EntityType.CREEPER, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setCharged(tag.getBoolean(POWERED_KEY, false));
        this.getEntityMeta().setIgnited(tag.getBoolean(IGNITED_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(POWERED_KEY, this.getEntityMeta().isCharged());
        tag.putBoolean(IGNITED_KEY, this.getEntityMeta().isIgnited());
    }
}
