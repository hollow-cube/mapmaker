package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.PiglinMeta;

import java.util.UUID;

public class PiglinEntity extends AbstractPiglinEntity<PiglinMeta> {

    public static final MapEntityInfo<PiglinEntity> INFO = MapEntityInfo.<PiglinEntity>builder(AbstractPiglinEntity.INFO)
        .with("Is Baby", MapEntityInfoType.Bool(false, PiglinMeta::setBaby, PiglinMeta::isBaby))
        .with("Is Dancing", MapEntityInfoType.Bool(false, PiglinMeta::setDancing, PiglinMeta::isDancing))
        .build();

    private static final String IS_BABY_KEY = "IsBaby";
    private static final String IS_DANCING_KEY = "mapmaker:is_dancing";

    public PiglinEntity(UUID uuid) {
        super(EntityType.PIGLIN, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setBaby(tag.getBoolean(IS_BABY_KEY, false));

        // Mapmaker
        this.getEntityMeta().setDancing(tag.getBoolean(IS_DANCING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(IS_BABY_KEY, this.getEntityMeta().isBaby());

        // Mapmaker
        tag.putBoolean(IS_DANCING_KEY, this.getEntityMeta().isDancing());
    }
}
