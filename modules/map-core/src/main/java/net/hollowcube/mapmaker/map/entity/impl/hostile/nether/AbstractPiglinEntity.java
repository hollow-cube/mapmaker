package net.hollowcube.mapmaker.map.entity.impl.hostile.nether;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.BasePiglinMeta;
import net.minestom.server.entity.metadata.monster.PiglinMeta;

import java.util.UUID;

public class AbstractPiglinEntity<M extends BasePiglinMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<AbstractPiglinEntity<? extends BasePiglinMeta>> INFO = MapEntityInfo.<AbstractPiglinEntity<? extends BasePiglinMeta>>builder(AbstractLivingEntity.INFO)
        .with("No Shake", MapEntityInfoType.Bool(false, BasePiglinMeta::setImmuneToZombification, BasePiglinMeta::isImmuneToZombification))
        .build();

    private static final String IS_IMMUNE_TO_ZOMBIFICATION_KEY = "IsImmuneToZombification";

    protected AbstractPiglinEntity(EntityType type, UUID uuid) {
        super(type, uuid);
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
