package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.AbstractIllagerMeta;
import net.minestom.server.entity.metadata.monster.raider.RaiderMeta;

import java.util.UUID;

public class AbstractIllagerEntity<M extends RaiderMeta> extends AbstractMobEntity<M> {

    public static final MapEntityInfo<AbstractIllagerEntity<? extends RaiderMeta>> INFO = MapEntityInfo.<AbstractIllagerEntity<? extends RaiderMeta>>builder(AbstractLivingEntity.INFO)
        .with("Celebrating", MapEntityInfoType.Bool(false, RaiderMeta::setCelebrating, RaiderMeta::isCelebrating))
        .build();

    private static final String CELEBRATING_KEY = "mapmaker:celebrating";

    protected AbstractIllagerEntity(EntityType type, UUID uuid) {
        super(type, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setCelebrating(tag.getBoolean(CELEBRATING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(CELEBRATING_KEY, this.getEntityMeta().isCelebrating());
    }
}
