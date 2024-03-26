package net.hollowcube.mapmaker.map.entity.metadata;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.MobMeta;
import org.jetbrains.annotations.NotNull;

final class BaseMetaSerializer {

    static void readEntityMeta(@NotNull EntityMeta meta, @NotNull CompoundBinaryTag tag) {
        meta.setOnFire(tag.getBoolean("HasVisualFire") || tag.getShort("Fire") > 0);
        meta.setHasGlowingEffect(tag.getBoolean("Glowing"));
        meta.setAirTicks(tag.getShort("Air"));
        var customNameStr = tag.getString("CustomName");
        if (!customNameStr.isBlank())
            meta.setCustomName(GsonComponentSerializer.gson().deserialize(customNameStr));
        meta.setCustomNameVisible(tag.getBoolean("CustomNameVisible"));
        meta.setSilent(tag.getBoolean("Silent"));
        meta.setHasNoGravity(tag.getBoolean("NoGravity"));
        meta.setTickFrozen(tag.getInt("TicksFrozen"));
    }

    static void writeEntityMeta(@NotNull EntityMeta meta, @NotNull CompoundBinaryTag.Builder tag) {
        tag.putBoolean("HasVisualFire", meta.isOnFire());
        tag.putBoolean("Glowing", meta.isHasGlowingEffect());
        tag.putShort("Air", (short) meta.getAirTicks());
        var customName = meta.getCustomName();
        if (customName != null)
            tag.putString("CustomName", GsonComponentSerializer.gson().serialize(customName));
        tag.putBoolean("CustomNameVisible", meta.isCustomNameVisible());
        tag.putBoolean("Silent", meta.isSilent());
        tag.putBoolean("NoGravity", meta.isHasNoGravity());
        tag.putInt("TicksFrozen", meta.getTickFrozen());
    }

    static void readMobMeta(@NotNull MobMeta meta, @NotNull CompoundBinaryTag tag) {
        meta.setNoAi(tag.getBoolean("NoAI"));
        meta.setLeftHanded(tag.getBoolean("LeftHanded"));
    }

    static void writeMobMeta(@NotNull MobMeta meta, @NotNull CompoundBinaryTag.Builder tag) {
        tag.putBoolean("NoAI", meta.isNoAi());
        tag.putBoolean("LeftHanded", meta.isLeftHanded());
    }

}
