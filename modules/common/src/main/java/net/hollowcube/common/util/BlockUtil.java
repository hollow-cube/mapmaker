package net.hollowcube.common.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.kyori.adventure.key.Key;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.block.BlockUtils;
import org.jetbrains.annotations.Nullable;
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
    private static final Int2ObjectMap<Material> BLOCK_TO_ITEM;
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

        var blockToItem = new Int2ObjectOpenHashMap<Material>();
        for (var block : Block.values()) {
            var material = block.registry().material();
            if (material == null) continue;
            blockToItem.put(block.id(), material);
        }
        BLOCK_TO_ITEM = blockToItem;
    }

    private static final IntSet ALWAYS_WATERLOGGED_BLOCKS = new IntOpenHashSet();
    static {
        ALWAYS_WATERLOGGED_BLOCKS.add(Block.TALL_SEAGRASS.id());
        ALWAYS_WATERLOGGED_BLOCKS.add(Block.SEAGRASS.id());
        ALWAYS_WATERLOGGED_BLOCKS.add(Block.BUBBLE_COLUMN.id());
        ALWAYS_WATERLOGGED_BLOCKS.add(Block.KELP.id());
        ALWAYS_WATERLOGGED_BLOCKS.add(Block.KELP_PLANT.id());
    }

    private BlockUtil() {
    }

    /**
     * Get the possible properties for a block.
     *
     * @param block The block to get the properties of.
     * @return Returns a map with the key being the property and the value being a list of possible values.
     */
    public static @Unmodifiable Map<String, String[]> getBlockProperties(Block block) {
        return Objects.requireNonNull(BLOCK_PROPERTIES.get(block.id()), "Block was not found in the valid properties map");
    }

    // No IntelliJ, get from a map that returns an object CAN return null
    @SuppressWarnings("DataFlowIssue")
    public static @Nullable Material getItem(Block block) {
        return BLOCK_TO_ITEM.get(block.id());
    }

    public static Block fromStringOld(String blockState) {
        return ArgumentBlockState.staticParse(blockState);
    }

    public static Either<Block, BlockParseResult> fromString(String input) {
        final int nbtIndex = input.indexOf("[");
        if (nbtIndex == 0) {
            return Either.right(BlockParseResult.NO_BLOCK_TYPE);
        }

        if (nbtIndex == -1) {
            // Only block name
            if (!Key.parseable(input))
                return Either.right(BlockParseResult.BLOCK_NOT_FOUND);
            final Block block = Block.fromKey(input);
            if (block == null)
                return Either.right(BlockParseResult.BLOCK_NOT_FOUND);
            return Either.left(block);
        } else {
            if (!input.endsWith("]"))
                return Either.right(BlockParseResult.INVALID_PROPERTIES);
            // Block state
            final String blockName = input.substring(0, nbtIndex);
            if (!Key.parseable(blockName))
                return Either.right(BlockParseResult.BLOCK_NOT_FOUND);
            Block block = Block.fromKey(blockName);
            if (block == null)
                return Either.right(BlockParseResult.BLOCK_NOT_FOUND);

            // Compute properties
            final String query = input.substring(nbtIndex);
            final var propertyMap = BlockUtils.parseProperties(query);
            try {
                return Either.left(block.withProperties(propertyMap));
            } catch (IllegalArgumentException e) {
                return Either.right(BlockParseResult.INVALID_PROPERTY_VALUE);
            }
        }
    }

    public static String toString(Block block) {
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

    public static boolean isWaterlogged(Block block) {
        return "true".equals(block.getProperty("waterlogged")) || ALWAYS_WATERLOGGED_BLOCKS.contains(block.id());
    }

    public static @Nullable Direction getFacing(Block block) {
        var facing = block.getProperty("facing");
        if (facing == null) return null;

        return switch (facing.toLowerCase(Locale.ROOT)) {
            case "north" -> Direction.NORTH;
            case "east" -> Direction.EAST;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            default -> null;
        };
    }

    public static @Nullable Block fromStateIdOrNull(int stateId) {
        try {
            return Block.fromStateId(stateId);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null; // Return null if the state ID is invalid
        }
    }

    public static @Nullable Block fromBlockIdOrNull(int blockId) {
        try {
            return Block.fromBlockId(blockId);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null; // Return null if the block ID is invalid
        }
    }

    public enum BlockParseResult {
        NO_BLOCK_TYPE, BLOCK_NOT_FOUND, INVALID_PROPERTIES, INVALID_PROPERTY_VALUE
    }

}
