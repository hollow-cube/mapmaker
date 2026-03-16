package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.EndermanMeta;

import java.util.UUID;

public class EndermanEntity extends AbstractMobEntity<EndermanMeta> {

    public static final MapEntityInfo<EndermanEntity> INFO = MapEntityInfo.<EndermanEntity>builder(AbstractLivingEntity.INFO)
        .with("Screaming", MapEntityInfoType.Bool(false, EndermanMeta::setScreaming, EndermanMeta::isScreaming))
        .build();

    private static final String SCREAMING_KEY = "mapmaker:screaming";

    public EndermanEntity(UUID uuid) {
        super(EntityType.ENDERMAN, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Mapmaker
        this.getEntityMeta().setScreaming(tag.getBoolean(SCREAMING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Mapmaker
        tag.putBoolean(SCREAMING_KEY, this.getEntityMeta().isScreaming());
    }
}
