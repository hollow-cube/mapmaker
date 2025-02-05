package net.hollowcube.mapmaker.map.entity.impl.living;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.tameable.WolfMeta;
import net.minestom.server.registry.DynamicRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WolfEntity extends AbstractAgeableEntity {

    public WolfEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType);
    }

    @Override
    public @NotNull WolfMeta getEntityMeta() {
        return (WolfMeta) super.getEntityMeta();
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        final var meta = getEntityMeta();
        if (tag.getBoolean("Sitting")) meta.setSitting(true);
        meta.setCollarColor(tag.getInt("CollarColor"));
        meta.setVariant(DynamicRegistry.Key.of(tag.getString("variant")));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        final var meta = getEntityMeta();
        if (meta.isSitting()) tag.putBoolean("Sitting", true);
        tag.putInt("CollarColor", meta.getCollarColor());
        tag.putString("variant", meta.getVariant().toString());
    }
}
