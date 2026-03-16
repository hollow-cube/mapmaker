package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractFishEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.fish.PufferfishMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PufferfishEntity extends AbstractFishEntity<PufferfishMeta> {

    public static final MapEntityInfo<@NotNull PufferfishEntity> INFO = MapEntityInfo.<PufferfishEntity>builder(AbstractFishEntity.INFO)
        .with("State", MapEntityInfoType.Enum(PufferfishMeta.State.class, PufferfishMeta.State.UNPUFFED, PufferfishMeta::setState, PufferfishMeta::getState))
        .build();

    private static final String STATE_KEY = "PuffState";

    public PufferfishEntity(@NotNull UUID uuid) {
        super(EntityType.PUFFERFISH, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setState(NbtUtilV2.readIntEnum(tag.get(STATE_KEY), PufferfishMeta.State.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putInt(STATE_KEY, this.getEntityMeta().getState().ordinal());
    }
}
