package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.GoatMeta;

import java.util.UUID;

public class GoatEntity extends AbstractAgeableEntity<GoatMeta> {

    public static final MapEntityInfo<GoatEntity> INFO = MapEntityInfo.<GoatEntity>builder(AbstractAgeableEntity.INFO)
        .with("Has Left Horn", MapEntityInfoType.Bool(false, GoatMeta::setLeftHorn, GoatMeta::hasLeftHorn))
        .with("Has Right Horn", MapEntityInfoType.Bool(false, GoatMeta::setRightHorn, GoatMeta::hasRightHorn))
        .build();

    private static final String HAS_LEFT_HORN_KEY = "HasLeftHorn";
    private static final String HAS_RIGHT_HORN_KEY = "HasRightHorn";

    public GoatEntity(UUID uuid) {
        super(EntityType.GOAT, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setLeftHorn(tag.getBoolean(HAS_LEFT_HORN_KEY, false));
        this.getEntityMeta().setRightHorn(tag.getBoolean(HAS_RIGHT_HORN_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(HAS_LEFT_HORN_KEY, this.getEntityMeta().hasLeftHorn());
        tag.putBoolean(HAS_RIGHT_HORN_KEY, this.getEntityMeta().hasRightHorn());
    }
}
