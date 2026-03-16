package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.ArmadilloMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArmadilloEntity extends AbstractAgeableEntity<ArmadilloMeta> {

    public static final MapEntityInfo<@NotNull ArmadilloEntity> INFO = MapEntityInfo.<ArmadilloEntity>builder(AbstractAgeableEntity.INFO)
        .with("State", MapEntityInfoType.Enum(ArmadilloMeta.State.class, ArmadilloMeta.State.IDLE, ArmadilloMeta::setState, ArmadilloMeta::getState))
        .build();

    private static final String STATE_KEY = "state";

    public ArmadilloEntity(@NotNull UUID uuid) {
        super(EntityType.ARMADILLO, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setState(NbtUtilV2.readStringEnum(tag.get(STATE_KEY), ArmadilloMeta.State.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(STATE_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getState()));
    }
}
