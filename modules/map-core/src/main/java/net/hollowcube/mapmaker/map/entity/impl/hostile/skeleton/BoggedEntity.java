package net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.skeleton.BoggedMeta;

import java.util.UUID;

public class BoggedEntity extends AbstractSkeletonEntity<BoggedMeta> {

    public static final MapEntityInfo<BoggedEntity> INFO = MapEntityInfo.<BoggedEntity>builder(AbstractSkeletonEntity.INFO)
        .with("Is Sheared", MapEntityInfoType.Bool(false, BoggedMeta::setSheared, BoggedMeta::isSheared))
        .build();

    private static final String SHEARED_KEY = "sheared";

    public BoggedEntity(UUID uuid) {
        super(EntityType.BOGGED, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setSheared(tag.getBoolean(SHEARED_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(SHEARED_KEY, this.getEntityMeta().isSheared());
    }
}
