package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.flying.GhastMeta;

import java.util.UUID;

public class GhastEntity extends AbstractMobEntity<GhastMeta> {

    public static final MapEntityInfo<GhastEntity> INFO = MapEntityInfo.<GhastEntity>builder(AbstractLivingEntity.INFO)
        .with("Mouth Open", MapEntityInfoType.Bool(false, GhastMeta::setAttacking, GhastMeta::isAttacking))
        .build();

    private static final String IS_ATTACKING_KEY = "mapmaker:is_attacking";

    public GhastEntity(UUID uuid) {
        super(EntityType.GHAST, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setAttacking(tag.getBoolean(IS_ATTACKING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(IS_ATTACKING_KEY, this.getEntityMeta().isAttacking());
    }
}
