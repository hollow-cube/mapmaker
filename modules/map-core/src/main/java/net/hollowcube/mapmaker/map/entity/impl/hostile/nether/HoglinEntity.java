package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.HoglinMeta;

import java.util.UUID;

public class HoglinEntity extends AbstractAgeableEntity<HoglinMeta> {

    public static final MapEntityInfo<HoglinEntity> INFO = MapEntityInfo.<HoglinEntity>builder(AbstractAgeableEntity.INFO)
        .with("No Shake", MapEntityInfoType.Bool(false, HoglinMeta::setImmuneToZombification, HoglinMeta::isImmuneToZombification))
        .build();

    private static final String IS_IMMUNE_TO_ZOMBIFICATION_KEY = "IsImmuneToZombification";

    public HoglinEntity(UUID uuid) {
        super(EntityType.HOGLIN, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setImmuneToZombification(tag.getBoolean(IS_IMMUNE_TO_ZOMBIFICATION_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(IS_IMMUNE_TO_ZOMBIFICATION_KEY, this.getEntityMeta().isImmuneToZombification());
    }
}
