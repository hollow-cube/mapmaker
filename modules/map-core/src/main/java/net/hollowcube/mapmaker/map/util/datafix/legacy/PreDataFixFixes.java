package net.hollowcube.mapmaker.map.util.datafix.legacy;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Metadata;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * Implements some manual data fixes from the 1.20.4 to 1.20.5 upgrade when data converter was added to do proper upgrades.
 */
@SuppressWarnings("UnstableApiUsage")
public final class PreDataFixFixes {

    public static int fixEntityMetaIndex1_20_4(int index) {
        return switch (index) {
            case 18 -> 19; // VILLAGERDATA
            case 19 -> 20; // OPTVARINT
            case 20 -> 21; // POSE
            case 21 -> 22; // CAT_VARIANT
            case 22 -> 24; // FROG_VARIANT
            case 23 -> 25; // OPT_GLOBAL_POSITION
            case 24 -> 26; // PAINTING_VARIANT
            case 25 -> 27; // SNIFFER_STATE
            case 26 -> 29; // VECTOR3
            case 27 -> 30; // QUATERNION
            default -> index;
        };
    }

    public static @NotNull Metadata.Entry<?> readEntityMeta1_20_4(int type, @NotNull NetworkBuffer buffer) {
        return switch (type) {
            // Read old ItemStack format
            case 7 -> Metadata.ItemStack(readItemStack1_20_4(buffer));
            default -> Metadata.Entry.read(type, buffer);
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
        CompoundBinaryTag updatedCompound = MCDataConverter.convertTag(MCTypeRegistry.ITEM_STACK, fullCompound, MCVersions.V1_20_4, MCVersions.V1_20_5_RC2);
        return ItemStack.NBT_TYPE.read(updatedCompound);
    }

    private PreDataFixFixes() {
    }
}
