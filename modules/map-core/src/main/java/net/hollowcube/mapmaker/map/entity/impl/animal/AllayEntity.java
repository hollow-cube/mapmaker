package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.AllayMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Can't exist due to Allays allowing items to be given to it on the client.
public class AllayEntity extends AbstractMobEntity<AllayMeta> {

    public static final MapEntityInfo<@NotNull AllayEntity> INFO = MapEntityInfo.<AllayEntity>builder(AbstractLivingEntity.INFO)
        .with("Dancing", MapEntityInfoType.Bool(false, AllayMeta::setDancing, AllayMeta::isDancing))
        .build();

    public AllayEntity(@NotNull UUID uuid) {
        super(EntityType.ALLAY, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);
    }
}
