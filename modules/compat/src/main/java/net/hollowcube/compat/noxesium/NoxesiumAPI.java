package net.hollowcube.compat.noxesium;

import com.noxcrew.noxesium.api.NoxesiumReferences;
import com.noxcrew.noxesium.api.protocol.NoxesiumFeature;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

public class NoxesiumAPI {

    public static final Component NAME = Component.text("Noxesium", NamedTextColor.WHITE);
    public static final Tag<Byte> NOXESIUM_VERSION = Tag.<Byte>Transient("noxesium:version")
            .defaultValue((byte) -1);
    public static final String CHANNEL = "noxesium-v2";

    private static final Tag<BinaryTag> BUKKIT_TAG = Tag.NBT(NoxesiumReferences.BUKKIT_COMPOUND_ID);
    private static final CompoundBinaryTag IMMUTABLE_TAG = CompoundBinaryTag.builder().putBoolean(NoxesiumReferences.IMMOVABLE_TAG, true).build();

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

    public static boolean canUseFeature(Player player, NoxesiumFeature feature) {
        return player.getTag(NOXESIUM_VERSION) >= feature.getMinProtocolVersion();
    }
}
