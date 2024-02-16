package net.hollowcube.map2.block;

import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A list of tags to various groups of blocks, taken from the block_tags.json from running <a href="https://github.com/hollow-cube/minestom-ce-data">Minestom CE's Data Generator</a>
 */
public final class BlockTags {
    public static final Collection<NamespaceID> LOGS = builtin("minecraft:logs");
    public static final Collection<NamespaceID> STAIRS = builtin("minecraft:stairs");
    public static final Collection<NamespaceID> WALLS = builtin("minecraft:walls");
    public static final Collection<NamespaceID> SLABS = builtin("minecraft:slabs");
    public static final Collection<NamespaceID> BUTTONS = builtin("minecraft:buttons");
    public static final Collection<NamespaceID> WOODEN_BUTTONS = create(Block.BIRCH_BUTTON, Block.ACACIA_BUTTON, Block.DARK_OAK_BUTTON, Block.JUNGLE_BUTTON, Block.MANGROVE_BUTTON, Block.OAK_BUTTON, Block.SPRUCE_BUTTON);
    public static final Collection<NamespaceID> STONE_BUTTONS = create(Block.POLISHED_BLACKSTONE_BUTTON, Block.STONE_BUTTON);
    public static final Collection<NamespaceID> NETHER_WOOD_BUTTONS = create(Block.WARPED_BUTTON, Block.CRIMSON_BUTTON);
    public static final Collection<NamespaceID> FENCES = builtin("minecraft:fences");
    public static final Collection<NamespaceID> WOODEN_FENCES = builtin("minecraft:wooden_fences");
    public static final Collection<NamespaceID> FENCE_GATES = builtin("minecraft:fence_gates");
    public static final Collection<NamespaceID> SIGNS = builtin("minecraft:signs"); // Standing + wall signs
    public static final Collection<NamespaceID> ALL_HANGING_SIGNS = builtin("minecraft:all_hanging_signs"); // Ceiling + wall hanging signs
    public static final Collection<NamespaceID> STANDING_SIGNS = builtin("minecraft:standing_signs");
    public static final Collection<NamespaceID> WALL_SIGNS = builtin("minecraft:wall_signs");
    public static final Collection<NamespaceID> CEILING_HANGING_SIGNS = builtin("minecraft:ceiling_hanging_signs");
    public static final Collection<NamespaceID> WALL_HANGING_SIGNS = builtin("minecraft:wall_hanging_signs");
    public static final Collection<NamespaceID> ANVILS = builtin("minecraft:anvil");
    public static final Collection<NamespaceID> TRAPDOORS = builtin("minecraft:trapdoors");
    public static final Collection<NamespaceID> CANDLES = builtin("minecraft:candles");
    public static final Collection<NamespaceID> CANDLE_CAKES = builtin("minecraft:candle_cakes");
    public static final Collection<NamespaceID> BANNERS = builtin("minecraft:banners");
    public static final Collection<NamespaceID> DOORS = builtin("minecraft:doors");
    public static final Collection<NamespaceID> TERRACOTTA = builtin("minecraft:terracotta");
    public static final Collection<NamespaceID> GLAZED_TERRACOTTA = create(
            Block.WHITE_GLAZED_TERRACOTTA,
            Block.ORANGE_GLAZED_TERRACOTTA,
            Block.MAGENTA_GLAZED_TERRACOTTA,
            Block.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Block.YELLOW_GLAZED_TERRACOTTA,
            Block.LIME_GLAZED_TERRACOTTA,
            Block.PINK_GLAZED_TERRACOTTA,
            Block.GRAY_GLAZED_TERRACOTTA,
            Block.LIGHT_GRAY_GLAZED_TERRACOTTA,
            Block.CYAN_GLAZED_TERRACOTTA,
            Block.PURPLE_GLAZED_TERRACOTTA,
            Block.BLUE_GLAZED_TERRACOTTA,
            Block.BROWN_GLAZED_TERRACOTTA,
            Block.GREEN_GLAZED_TERRACOTTA,
            Block.RED_GLAZED_TERRACOTTA,
            Block.BLACK_GLAZED_TERRACOTTA
    );
    public static final Collection<NamespaceID> SMALL_FLOWERS = builtin("minecraft:small_flowers");
    public static final Collection<NamespaceID> FLOWER_POTS = builtin("minecraft:flower_pots");
    public static final Collection<NamespaceID> BEDS = builtin("minecraft:beds");
    public static final Collection<NamespaceID> LEAVES = builtin("minecraft:leaves");
    public static final Collection<NamespaceID> SHULKER_BOXES = builtin("minecraft:shulker_boxes");
    public static final Collection<NamespaceID> SKULLS = create(
            Block.SKELETON_SKULL, Block.SKELETON_WALL_SKULL,
            Block.WITHER_SKELETON_SKULL, Block.WITHER_SKELETON_WALL_SKULL,
            Block.ZOMBIE_HEAD, Block.ZOMBIE_WALL_HEAD,
            Block.CREEPER_HEAD, Block.CREEPER_WALL_HEAD,
            Block.DRAGON_HEAD, Block.DRAGON_WALL_HEAD,
            Block.PIGLIN_HEAD, Block.PIGLIN_WALL_HEAD
    );
    public static final Collection<NamespaceID> GLASS_PANES = create(
            Block.GLASS_PANE,
            Block.WHITE_STAINED_GLASS_PANE,
            Block.LIGHT_GRAY_STAINED_GLASS_PANE,
            Block.GRAY_STAINED_GLASS_PANE,
            Block.BLACK_STAINED_GLASS_PANE,
            Block.BROWN_STAINED_GLASS_PANE,
            Block.RED_STAINED_GLASS_PANE,
            Block.ORANGE_STAINED_GLASS_PANE,
            Block.YELLOW_STAINED_GLASS_PANE,
            Block.LIME_STAINED_GLASS_PANE,
            Block.GREEN_STAINED_GLASS_PANE,
            Block.CYAN_STAINED_GLASS_PANE,
            Block.LIGHT_BLUE_STAINED_GLASS_PANE,
            Block.BLUE_STAINED_GLASS_PANE,
            Block.PURPLE_STAINED_GLASS_PANE,
            Block.MAGENTA_STAINED_GLASS_PANE,
            Block.PINK_STAINED_GLASS_PANE
    );
    public static final Collection<NamespaceID> POTTABLE_FLOWERS = create(() -> {
        var flowers = new HashSet<Block>();
        for (var flower : FLOWER_POTS) {
            if (flower.asString().equals("minecraft:flower_pot")) continue;
            var block = Block.fromNamespaceId(flower.asString().replace("potted_", ""));
            if (block == null) continue;
            flowers.add(block);
        }
        return flowers;
    });
    public static final Collection<NamespaceID> TALL_FLOWERS = create(
            Block.TALL_GRASS,
            Block.ROSE_BUSH,
            Block.LILAC,
            Block.SUNFLOWER,
            Block.LARGE_FERN
    );
    public static final Collection<NamespaceID> ANY_WITH_LIT = createFromProperty("lit");
    public static Collection<NamespaceID> FARMLAND_CONVERTABLE = create(Block.DIRT, Block.GRASS_BLOCK, Block.DIRT_PATH);
    public static Collection<NamespaceID> DIRT_CONVERTABLE = create(Block.ROOTED_DIRT, Block.COARSE_DIRT);
    public static Collection<NamespaceID> DIRT_PATH_CONVERTABLE = create(
            Block.GRASS_BLOCK, Block.DIRT, Block.MYCELIUM,
            Block.PODZOL, Block.COARSE_DIRT, Block.ROOTED_DIRT
    );
    public static Collection<NamespaceID> CORAL = create(
            Block.TUBE_CORAL, Block.BRAIN_CORAL, Block.BUBBLE_CORAL,
            Block.FIRE_CORAL, Block.HORN_CORAL,
            Block.DEAD_TUBE_CORAL, Block.DEAD_BRAIN_CORAL, Block.DEAD_BUBBLE_CORAL,
            Block.DEAD_FIRE_CORAL, Block.DEAD_HORN_CORAL
    );
    public static Collection<NamespaceID> CORAL_FAN = create(
            Block.TUBE_CORAL_FAN, Block.BRAIN_CORAL_FAN, Block.BUBBLE_CORAL_FAN,
            Block.FIRE_CORAL_FAN, Block.HORN_CORAL_FAN,
            Block.DEAD_TUBE_CORAL_FAN, Block.DEAD_BRAIN_CORAL_FAN, Block.DEAD_BUBBLE_CORAL_FAN,
            Block.DEAD_FIRE_CORAL_FAN, Block.DEAD_HORN_CORAL_FAN
    );
    public static final Collection<NamespaceID> GROWABLE = create( //todo for bonemeal
            Block.TORCHFLOWER_CROP, Block.MELON_STEM, Block.PUMPKIN_STEM,
            Block.WHEAT, Block.CARROTS, Block.NETHER_WART, Block.POTATOES,
            Block.PITCHER_CROP, Block.BEETROOTS, Block.COCOA
    );

    private static @NotNull Collection<NamespaceID> builtin(@NotNull String name) {
        var tag = MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, name);
        Check.notNull(tag, "Tag " + name + " is not registered");
        return tag.getValues();
    }

    private static @NotNull Collection<NamespaceID> create(@NotNull Block... block) {
        var set = new HashSet<NamespaceID>();
        for (var b : block) {
            set.add(b.namespace());
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<NamespaceID> create(@NotNull Supplier<Set<Block>> blocks) {
        var set = new HashSet<NamespaceID>();
        for (var b : blocks.get()) {
            set.add(b.namespace());
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<NamespaceID> createFromProperty(@NotNull String property) {
        var set = new HashSet<NamespaceID>();
        for (var block : Block.values()) {
            if (block.getProperty(property) != null) {
                set.add(block.namespace());
            }
        }
        return Set.copyOf(set);
    }

}
