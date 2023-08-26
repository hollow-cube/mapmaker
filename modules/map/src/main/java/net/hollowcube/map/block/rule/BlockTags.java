package net.hollowcube.map.block.rule;

import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.block.Block;

import java.util.Objects;
import java.util.Set;

/**
 * A list of tags to various groups of blocks, taken from the block_tags.json from running <a href="https://github.com/hollow-cube/minestom-ce-data">Minestom CE's Data Generator</a>
 */
public final class BlockTags {
    private static final TagManager TAG_MANAGER = MinecraftServer.getTagManager();

    public static final Tag MINECRAFT_STAIRS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:stairs"));

    public static final Tag WALLS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:walls"));

    public static final Tag MINECRAFT_SLABS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:slabs"));

    public static final Tag MINECRAFT_BUTTONS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:buttons"));

    public static final Tag FENCES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:fences"));
    public static final Tag WOODEN_FENCES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:wooden_fences"));
    public static final Tag MINECRAFT_FENCE_GATES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:fence_gates"));

    public static final Tag MINECRAFT_WALL_SIGNS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:wall_signs"));
    public static final Tag MINECRAFT_STANDING_SIGNS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:standing_signs"));

    public static final Tag MINECRAFT_ANVILS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:anvil"));

    public static final Tag MINECRAFT_TRAPDOORS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:trapdoors"));

    public static final Tag MINECRAFT_CANDLES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:candles"));

    public static final Tag MINECRAFT_BANNERS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:banners"));

    public static final Tag MINECRAFT_DOORS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:doors"));

    public static final Tag SMALL_FLOWERS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:small_flowers"));

    public static final Tag MINECRAFT_BEDS = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:beds"));

    public static final Tag LEAVES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:leaves"));

    public static final Tag SHULKER_BOXES = Objects.requireNonNull(TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, "minecraft:shulker_boxes"));

    public static final Set<Integer> PANES = Set.of(
            Block.GLASS_PANE.id(),
            Block.WHITE_STAINED_GLASS_PANE.id(),
            Block.LIGHT_GRAY_STAINED_GLASS_PANE.id(),
            Block.GRAY_STAINED_GLASS_PANE.id(),
            Block.BLACK_STAINED_GLASS_PANE.id(),
            Block.BROWN_STAINED_GLASS_PANE.id(),
            Block.RED_STAINED_GLASS_PANE.id(),
            Block.ORANGE_STAINED_GLASS_PANE.id(),
            Block.YELLOW_STAINED_GLASS_PANE.id(),
            Block.LIME_STAINED_GLASS_PANE.id(),
            Block.GREEN_STAINED_GLASS_PANE.id(),
            Block.CYAN_STAINED_GLASS_PANE.id(),
            Block.LIGHT_BLUE_STAINED_GLASS_PANE.id(),
            Block.BLUE_STAINED_GLASS_PANE.id(),
            Block.PURPLE_STAINED_GLASS_PANE.id(),
            Block.MAGENTA_STAINED_GLASS_PANE.id(),
            Block.PINK_STAINED_GLASS_PANE.id(),
            Block.IRON_BARS.id()
    );

}
