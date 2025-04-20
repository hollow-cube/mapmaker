package net.hollowcube.mapmaker.map.entity.impl.living;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.golem.ShulkerMeta;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ShulkerEntity extends AbstractMobEntity {

    public ShulkerEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public @NotNull ShulkerMeta getEntityMeta() {
        return (ShulkerMeta) super.getEntityMeta();
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        set(DataComponents.SHULKER_COLOR, DyeColor.values()[tag.getByte("Color", (byte) 16)]);

        final var meta = getEntityMeta();
        meta.setAttachFace(Direction.values()[tag.getByte("AttachFace")]);
        meta.setShieldHeight(tag.getByte("Peek"));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        tag.putByte("Color", (byte) get(DataComponents.SHULKER_COLOR, DyeColor.PURPLE).ordinal());

        final var meta = getEntityMeta();
        tag.putByte("AttachFace", (byte) meta.getAttachFace().ordinal());
        tag.putByte("Peek", meta.getShieldHeight());
    }
}
