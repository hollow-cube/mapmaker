package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.SnifferMeta;
import net.minestom.server.entity.metadata.animal.StriderMeta;
import net.minestom.server.entity.metadata.water.fish.PufferfishMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class StriderEntity extends AbstractAgeableEntity<StriderMeta> {

    public static final MapEntityInfo<@NotNull StriderEntity> INFO = MapEntityInfo.<StriderEntity>builder(AbstractAgeableEntity.INFO)
        .with("Saddled", CommonMapEntityInfoTypes.Saddle())
        .build();

    public StriderEntity(@NotNull UUID uuid) {
        super(EntityType.STRIDER, uuid);
    }
}
