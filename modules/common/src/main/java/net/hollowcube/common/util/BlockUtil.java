package net.hollowcube.common.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class BlockUtil {

    // This comparator makes it so that when a property value is a number, it is compared as a number
    // instead of a string. This is so that 1 is always after 0, and 10 is always after 9.
    // This then compares the values as strings, but if one is "true" and the other is "false", "true" is greater.
    private static final Comparator<String> PROPERTY_VALUE_COMPARATOR = (o1, o2) -> {
        o1 = o1.toLowerCase();
        o2 = o2.toLowerCase();

        try {
            return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
        } catch (NumberFormatException e) {
            if (o1.equals("true") && o2.equals("false")) return 1;
            if (o1.equals("false") && o2.equals("true")) return -1;
            return o1.compareTo(o2);
        }
    };
    private static final Int2ObjectMap<Map<String, String[]>> BLOCK_PROPERTIES;

    static {
        var blockmap = new Int2ObjectOpenHashMap<Map<String, String[]>>();
        for (var block : Block.values()) {
            var blockprops = new HashMap<String, String[]>();
            for (var propName : block.properties().keySet()) {
                Set<String> propValues = new HashSet<>();
                for (var state : block.possibleStates()) {
                    propValues.add(state.getProperty(propName));
                }
                String[] values = propValues.toArray(new String[0]);
                Arrays.sort(values, PROPERTY_VALUE_COMPARATOR);
                blockprops.put(propName, values);
            }
            blockmap.put(block.id(), Collections.unmodifiableMap(blockprops));
        }
        BLOCK_PROPERTIES = blockmap;
    }

    /**
     * Get the possible properties for a block.
     * @param block The block to get the properties of.
     * @return Returns a map with the key being the property and the value being a list of possible values.
     */
    public static @NotNull @Unmodifiable Map<String, String[]> getBlockProperties(@NotNull Block block) {
        return Objects.requireNonNull(BLOCK_PROPERTIES.get(block.id()), "Block was not found in the valid properties map");
    }

    public static @NotNull Block fromString(@NotNull String blockState) {
        return ArgumentBlockState.staticParse(blockState);
    }

    public static @NotNull String toString(@NotNull Block block) {
        var builder = new StringBuilder(block.name());
        if (block.properties().isEmpty()) return builder.toString();

        builder.append('[');
        for (var entry : block.properties().entrySet()) {
            builder.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append(',');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(']');

        return builder.toString();
    }

}
