package net.hollowcube.compat.noxesium;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

public class NoxesiumAPI {

    public static final String NAMESPACE = "noxesium";
    public static final Component NAME = Component.text("Noxesium", NamedTextColor.WHITE);

    private static final Tag<BinaryTag> BUKKIT_TAG = Tag.NBT("PublicBukkitValues");
    private static final CompoundBinaryTag IMMUTABLE_TAG = CompoundBinaryTag.builder().putBoolean("noxesium:immovable", true).build();

    public static ItemStack.Builder setImmovable(ItemStack.Builder builder) {
        return builder.set(BUKKIT_TAG, IMMUTABLE_TAG);
    }

    public static ItemStack setImmovable(ItemStack stack) {
        return stack.with(NoxesiumAPI::setImmovable);
    }

    public static CustomData setImmovable(@Nullable CustomData data) {
        data = data == null ? CustomData.EMPTY : data;
        return data.withTag(BUKKIT_TAG, IMMUTABLE_TAG);
    }
}
