package net.hollowcube.mapmaker.map.util.datafix.legacy;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataTypes;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.EntityMetadataStealer;
import net.minestom.server.entity.Metadata;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * Implements some manual data fixes from the 1.20.4 to 1.20.5 upgrade when data converter was added to do proper upgrades.
 */
public final class PreDataFixFixes {

    public static int fixEntityMetaIndex1_20_4(int index) {
        return switch (index) {
            case 18 -> Metadata.TYPE_VILLAGERDATA; // VILLAGERDATA
            case 19 -> Metadata.TYPE_OPT_VARINT; // OPTVARINT
            case 20 -> Metadata.TYPE_POSE; // POSE
            case 21 -> Metadata.TYPE_CAT_VARIANT; // CAT_VARIANT
            case 22 -> Metadata.TYPE_FROG_VARIANT; // FROG_VARIANT
            case 23 -> Metadata.TYPE_OPT_GLOBAL_POSITION; // OPT_GLOBAL_POSITION
            case 24 -> Metadata.TYPE_PAINTING_VARIANT; // PAINTING_VARIANT
            case 25 -> Metadata.TYPE_SNIFFER_STATE; // SNIFFER_STATE
            case 26 -> Metadata.TYPE_VECTOR3; // VECTOR3
            case 27 -> Metadata.TYPE_QUATERNION; // QUATERNION
            default -> index;
        };
    }

    public static @NotNull Metadata.Entry<?> readEntityMeta1_20_4(int type, @NotNull NetworkBuffer buffer) {
        return switch (type) {
            // Read old ItemStack format
            case 7 -> Metadata.ItemStack(readItemStack1_20_4(buffer));
            default -> EntityMetadataStealer.legacyReadEntry(buffer, type);
        };
    }

    public static @NotNull ItemStack readItemStack1_20_4(@NotNull NetworkBuffer buffer) {
        boolean present = buffer.read(NetworkBuffer.BOOLEAN);
        if (!present) return ItemStack.AIR;

        int id = buffer.read(NetworkBuffer.VAR_INT);
        final Material material = Material.fromId(id);
        if (material == null) throw new RuntimeException("Unknown material id: " + id);

        final int amount = buffer.read(NetworkBuffer.BYTE);
        final BinaryTag nbt = buffer.read(NetworkBuffer.NBT);
        if (!(nbt instanceof CompoundBinaryTag compound)) {
            return ItemStack.of(material, amount);
        }

        // Update to item components
        CompoundBinaryTag fullCompound = CompoundBinaryTag.builder()
                .put(compound)
                .putString("id", material.name())
                .putInt("Count", amount)
                .build();
        CompoundBinaryTag updatedCompound = (CompoundBinaryTag) DataFixer.upgrade(DataTypes.ITEM_STACK,
                Transcoder.NBT, fullCompound, 4189, MinecraftServer.DATA_VERSION);
        return ItemStack.fromItemNBT(updatedCompound);
    }

    private PreDataFixFixes() {
    }
}
