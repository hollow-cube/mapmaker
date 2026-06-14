package net.hollowcube.mapmaker.map.entity.impl.hostile;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityMetadataStealer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.metadata.golem.ShulkerMeta;
import net.minestom.server.utils.Direction;

import java.util.UUID;

public class ShulkerEntity extends AbstractMobEntity<ShulkerMeta> {

    public static final MapEntityInfo<ShulkerEntity> INFO = MapEntityInfo.<ShulkerEntity>builder(AbstractLivingEntity.INFO)
        .with("Color", MapEntityInfoType.NullableEnum(DyeColor.class, null, DataComponents.SHULKER_COLOR))
        .with("Attach Face", MapEntityInfoType.Enum(Direction.class, Direction.DOWN, ShulkerMeta::setAttachFace, ShulkerMeta::getAttachFace))
        .build();

    public ShulkerEntity(UUID uuid) {
        super(EntityType.SHULKER, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        var metadata = EntityMetadataStealer.steal(this);
        metadata.set(MetadataDef.Shulker.COLOR, tag.getByte("Color", (byte) 16));

        final var meta = getEntityMeta();
        meta.setAttachFace(Direction.values()[tag.getByte("AttachFace")]);
        meta.setShieldHeight(tag.getByte("Peek"));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        var metadata = EntityMetadataStealer.steal(this);
        tag.putByte("Color", metadata.get(MetadataDef.Shulker.COLOR));

        final var meta = getEntityMeta();
        tag.putByte("AttachFace", (byte) meta.getAttachFace().ordinal());
        tag.putByte("Peek", meta.getShieldHeight());
    }
}
