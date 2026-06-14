package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.WardenMeta;
import net.minestom.server.entity.metadata.monster.ZoglinMeta;

import java.util.UUID;

public class ZoglinEntity extends AbstractMobEntity<ZoglinMeta> {

    public static final MapEntityInfo<ZoglinEntity> INFO = MapEntityInfo.<ZoglinEntity>builder(AbstractLivingEntity.INFO)
        .with("Is Baby", MapEntityInfoType.Bool(false, ZoglinMeta::setBaby, ZoglinMeta::isBaby))
        .build();

    private static final String IS_BABY_KEY = "IsBaby";

    public ZoglinEntity(UUID uuid) {
        super(EntityType.ZOGLIN, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setBaby(tag.getBoolean(IS_BABY_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(IS_BABY_KEY, this.getEntityMeta().isBaby());
    }
}
