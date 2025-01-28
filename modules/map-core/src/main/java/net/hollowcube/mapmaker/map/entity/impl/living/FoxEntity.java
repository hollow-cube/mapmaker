package net.hollowcube.mapmaker.map.entity.impl.living;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.FoxMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FoxEntity extends AbstractAgeableEntity {
    public FoxEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType);
    }

    @Override
    public @NotNull FoxMeta getEntityMeta() {
        return (FoxMeta) super.getEntityMeta();
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        final var meta = getEntityMeta();
        if (tag.getBoolean("Crouching")) meta.setPouncing(true);
        if (tag.getBoolean("Sitting")) meta.setSitting(true);
        if (tag.getBoolean("Sleeping")) meta.setSleeping(true);
        final var typeName = tag.getString("Type");
        if (!typeName.isEmpty()) meta.setType(FoxMeta.Type.valueOf(typeName));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        final var meta = getEntityMeta();
        if (meta.isPouncing()) tag.putBoolean("Crouching", true);
        if (meta.isSitting()) tag.putBoolean("Sitting", true);
        if (meta.isSleeping()) tag.putBoolean("Sleeping", true);
        tag.putString("Type", meta.getType().name());
    }
}
