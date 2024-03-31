package net.hollowcube.mapmaker.map.item;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.Objects;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static @NotNull NBTCompound getEntityTag(@NotNull ItemStack itemStack) {
        var root = itemStack.meta().toNBT();
        return Objects.requireNonNullElse(root.getCompound("EntityTag"), new NBTCompound());
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var namespace = material.namespace();
        var translationKey = String.format("item.%s.%s", namespace.namespace(), namespace.path());
        return Component.translatable(translationKey);
    }
}
