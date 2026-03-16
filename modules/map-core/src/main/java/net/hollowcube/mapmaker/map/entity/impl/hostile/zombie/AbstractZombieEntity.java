package net.hollowcube.mapmaker.map.entity.impl.hostile.zombie;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta;

import java.util.UUID;

public class AbstractZombieEntity<M extends ZombieMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<AbstractZombieEntity<? extends ZombieMeta>> INFO = MapEntityInfo.<AbstractZombieEntity<? extends ZombieMeta>>builder(AbstractLivingEntity.INFO)
        .with("Is Baby", MapEntityInfoType.Bool(false, ZombieMeta::setBaby, ZombieMeta::isBaby))
        .with("Is Drowning", MapEntityInfoType.Bool(false, ZombieMeta::setBecomingDrowned, ZombieMeta::isBecomingDrowned))
        .build();

    private static final String BABY_KEY = "IsBaby";
    private static final String DROWNED_CONVERSION_KEY = "DrownedConversionTime";

    protected AbstractZombieEntity(EntityType type, UUID uuid) {
        super(type, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setBaby(tag.getBoolean(BABY_KEY, false));
        this.getEntityMeta().setBecomingDrowned(tag.getInt(DROWNED_CONVERSION_KEY, -1) != -1);
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(BABY_KEY, this.getEntityMeta().isBaby());
        tag.putInt(DROWNED_CONVERSION_KEY, this.getEntityMeta().isBecomingDrowned() ? 0 : -1);
    }
}
