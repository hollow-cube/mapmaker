package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.item.ItemStackFixer;
import net.minestom.server.entity.Metadata;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class MetadataIndexFixer {

    public static int fix1_20_4to1_20_5(int index) {
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

    public static @NotNull Metadata.Entry<?> read1_20_4to1_20_5(int type, @NotNull NetworkBuffer buffer) {
        return switch (type) {
            // Read old ItemStack format
            case 7 -> Metadata.ItemStack(ItemStackFixer.read1_20_4(buffer));
            default -> Metadata.Entry.read(type, buffer);
        };
    }

}
