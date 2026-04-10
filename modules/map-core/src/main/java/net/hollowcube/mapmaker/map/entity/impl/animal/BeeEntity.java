package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.BeeMeta;

import java.util.UUID;

public class BeeEntity extends AbstractAgeableEntity<BeeMeta> {

    public static final MapEntityInfo<BeeEntity> INFO = MapEntityInfo.<BeeEntity>builder(AbstractAgeableEntity.INFO)
        .with("Is Rolling", MapEntityInfoType.Bool(false, BeeMeta::setRolling, BeeMeta::isRolling))
        .with("Is Angry", MapEntityInfoType.Bool(
            false,
            (meta, isAngry) -> meta.setAngerEndTime(isAngry ? Integer.MAX_VALUE : 0),
            (meta) -> meta.getAngerEndTime() > 0
        ))
        .with("Has Nectar", MapEntityInfoType.Bool(false, BeeMeta::setHasNectar, BeeMeta::isHasNectar))
        .with("Has Stung", MapEntityInfoType.Bool(false, BeeMeta::setHasStung, BeeMeta::isHasStung))
        .build();

    private static final String HAS_STUNG_KEY = "HasStung";
    private static final String HAS_NECTAR_KEY = "HasNectar";
    private static final String ANGER_END_TIME_KEY = "anger_end_time";
    private static final String IS_ROLLING = "mapmaker:is_rolling";

    public BeeEntity(UUID uuid) {
        super(EntityType.BEE, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setHasStung(tag.getBoolean(HAS_STUNG_KEY, false));
        this.getEntityMeta().setHasNectar(tag.getBoolean(HAS_NECTAR_KEY, false));
        this.getEntityMeta().setAngerEndTime(tag.getLong(ANGER_END_TIME_KEY, 0));

        // Mapmaker
        this.getEntityMeta().setRolling(tag.getBoolean(IS_ROLLING, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(HAS_STUNG_KEY, this.getEntityMeta().isHasStung());
        tag.putBoolean(HAS_NECTAR_KEY, this.getEntityMeta().isHasNectar());
        tag.putLong(ANGER_END_TIME_KEY, this.getEntityMeta().getAngerEndTime());

        // Mapmaker
        tag.putBoolean(IS_ROLLING, this.getEntityMeta().isRolling());
    }
}
