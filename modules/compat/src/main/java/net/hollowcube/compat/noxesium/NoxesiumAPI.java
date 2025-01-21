package net.hollowcube.compat.noxesium;

import com.noxcrew.noxesium.api.NoxesiumReferences;
import com.noxcrew.noxesium.api.protocol.NoxesiumFeature;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

public class NoxesiumAPI {

    public static final Tag<Byte> NOXESIUM_VERSION = Tag.Transient("noxesium:version");
    public static final String CHANNEL = "noxesium-v2";

    private static final Tag<BinaryTag> BUKKIT_TAG = Tag.NBT(NoxesiumReferences.BUKKIT_COMPOUND_ID);
    private static final CompoundBinaryTag IMMUTABLE_TAG = CompoundBinaryTag.builder().putBoolean(NoxesiumReferences.IMMOVABLE_TAG, true).build();

    public static ItemStack.Builder setImmovable(ItemStack.Builder builder) {
        return builder.set(BUKKIT_TAG, IMMUTABLE_TAG);
    }

    public static ItemStack setImmovable(ItemStack stack) {
        return stack.with(NoxesiumAPI::setImmovable);
    }

    public static boolean canUseFeature(Player player, NoxesiumFeature feature) {
        Byte version = player.getTag(NOXESIUM_VERSION);
        return version != null && version >= feature.getMinProtocolVersion();
    }
}
