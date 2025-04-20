package net.hollowcube.mapmaker.map.entity.impl.living;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.tameable.WolfMeta;
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

        set(DataComponents.WOLF_COLLAR, DyeColor.values()[tag.getInt("CollarColor")]);
        var variantTag = tag.get("variant");
        if (variantTag != null) set(DataComponents.WOLF_VARIANT,
                DataComponents.WOLF_VARIANT.decode(Transcoder.NBT, variantTag).orElseThrow());
        var soundVariantTag = tag.get("sound_variant");
        if (soundVariantTag != null) set(DataComponents.WOLF_SOUND_VARIANT,
                DataComponents.WOLF_SOUND_VARIANT.decode(Transcoder.NBT, soundVariantTag).orElseThrow());

        final var meta = getEntityMeta();
        if (tag.getBoolean("Sitting")) meta.setSitting(true);
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        tag.putInt("CollarColor", get(DataComponents.WOLF_COLLAR).ordinal());
        tag.put("variant", DataComponents.WOLF_VARIANT.encode(Transcoder.NBT, get(DataComponents.WOLF_VARIANT)).orElseThrow());
        tag.put("sound_variant", DataComponents.WOLF_SOUND_VARIANT.encode(Transcoder.NBT, get(DataComponents.WOLF_SOUND_VARIANT)).orElseThrow());

        final var meta = getEntityMeta();
        if (meta.isSitting()) tag.putBoolean("Sitting", true);
    }
}
