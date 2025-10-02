package net.hollowcube.mapmaker.map.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

/**
 * A list of tags to various groups of blocks, taken from the block_tags.json from running <a href="https://github.com/hollow-cube/minestom-ce-data">Minestom CE's Data Generator</a>
 */
public final class BlockTags {
    public static final Collection<Key> LOGS = builtin("minecraft:logs");
    public static final Collection<Key> STAIRS = extend(builtin("minecraft:stairs"),
            Block.TUFF_STAIRS,
            Block.POLISHED_TUFF_STAIRS,
            Block.TUFF_BRICK_STAIRS
    );
    public static final Collection<Key> WALLS = extend(builtin("minecraft:walls"),
            Block.TUFF_WALL,
            Block.POLISHED_TUFF_WALL,
            Block.TUFF_BRICK_WALL
    );
    public static final Collection<Key> SLABS = extend(builtin("minecraft:slabs"),
            Block.TUFF_SLAB,
            Block.POLISHED_TUFF_SLAB,
            Block.TUFF_BRICK_SLAB
    );
    public static final Collection<Key> BUTTONS = builtin("minecraft:buttons");
    public static final Collection<Key> WOODEN_BUTTONS = create(Block.BIRCH_BUTTON, Block.ACACIA_BUTTON, Block.DARK_OAK_BUTTON, Block.JUNGLE_BUTTON, Block.MANGROVE_BUTTON, Block.OAK_BUTTON, Block.SPRUCE_BUTTON, Block.PALE_OAK_BUTTON);
    public static final Collection<Key> STONE_BUTTONS = create(Block.POLISHED_BLACKSTONE_BUTTON, Block.STONE_BUTTON);
    public static final Collection<Key> NETHER_WOOD_BUTTONS = create(Block.WARPED_BUTTON, Block.CRIMSON_BUTTON);
    public static final Collection<Key> FENCES = builtin("minecraft:fences");
    public static final Collection<Key> WOODEN_FENCES = builtin("minecraft:wooden_fences");
    public static final Collection<Key> FENCE_GATES = builtin("minecraft:fence_gates");
    public static final Collection<Key> SIGNS = builtin("minecraft:signs"); // Standing + wall signs
    public static final Collection<Key> ALL_HANGING_SIGNS = builtin("minecraft:all_hanging_signs"); // Ceiling + wall hanging signs
    public static final Collection<Key> STANDING_SIGNS = builtin("minecraft:standing_signs");
    public static final Collection<Key> WALL_SIGNS = builtin("minecraft:wall_signs");
    public static final Collection<Key> CEILING_HANGING_SIGNS = builtin("minecraft:ceiling_hanging_signs");
    public static final Collection<Key> WALL_HANGING_SIGNS = builtin("minecraft:wall_hanging_signs");
    public static final Collection<Key> ANVILS = builtin("minecraft:anvil");
    public static final Collection<Key> TRAPDOORS = extend(builtin("minecraft:trapdoors"));
    public static final Collection<Key> CANDLES = builtin("minecraft:candles");
    public static final Collection<Key> CANDLE_CAKES = builtin("minecraft:candle_cakes");
    public static final Collection<Key> BANNERS = builtin("minecraft:banners");
    public static final Collection<Key> DOORS = extend(builtin("minecraft:doors"));
    public static final Collection<Key> TERRACOTTA = builtin("minecraft:terracotta");
    public static final Collection<Key> GLAZED_TERRACOTTA = create(
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
    public static final Collection<Key> SMALL_FLOWERS = builtin("minecraft:small_flowers");
    public static final Collection<Key> FLOWER_POTS = builtin("minecraft:flower_pots");
    public static final Collection<Key> BEDS = builtin("minecraft:beds");
    public static final Collection<Key> LEAVES = builtin("minecraft:leaves");
    public static final Collection<Key> SHULKER_BOXES = builtin("minecraft:shulker_boxes");
    public static final Collection<Key> SKULLS = create(
            Block.SKELETON_SKULL, Block.SKELETON_WALL_SKULL,
            Block.WITHER_SKELETON_SKULL, Block.WITHER_SKELETON_WALL_SKULL,
            Block.ZOMBIE_HEAD, Block.ZOMBIE_WALL_HEAD,
            Block.CREEPER_HEAD, Block.CREEPER_WALL_HEAD,
            Block.DRAGON_HEAD, Block.DRAGON_WALL_HEAD,
            Block.PIGLIN_HEAD, Block.PIGLIN_WALL_HEAD
    );
    public static final Collection<Key> GLASS_PANES = create(
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
    public static final Collection<Key> POTTABLE_FLOWERS = create(() -> {
        var flowers = new HashSet<Block>();
        for (var flower : FLOWER_POTS) {
            if (flower.asString().equals("minecraft:flower_pot")) continue;
            var block = Block.fromKey(flower.asString().replace("potted_", ""));
            if (block == null) continue;
            flowers.add(block);
        }
        return flowers;
    });
    public static final Collection<Key> TALL_FLOWERS = create(
            Block.TALL_GRASS,
            Block.ROSE_BUSH,
            Block.LILAC,
            Block.SUNFLOWER,
            Block.LARGE_FERN,
            Block.PEONY,
            Block.PITCHER_PLANT
    );
    public static final Collection<Key> ANY_WITH_LIT = createFromProperty("lit");
    public static Collection<Key> FARMLAND_CONVERTABLE = create(Block.DIRT, Block.GRASS_BLOCK, Block.DIRT_PATH);
    public static Collection<Key> DIRT_CONVERTABLE = create(Block.ROOTED_DIRT, Block.COARSE_DIRT);
    public static Collection<Key> DIRT_PATH_CONVERTABLE = create(
            Block.GRASS_BLOCK, Block.DIRT, Block.MYCELIUM,
            Block.PODZOL, Block.COARSE_DIRT, Block.ROOTED_DIRT
    );
    public static Collection<Key> CORAL = create(
            Block.TUBE_CORAL, Block.BRAIN_CORAL, Block.BUBBLE_CORAL,
            Block.FIRE_CORAL, Block.HORN_CORAL,
            Block.DEAD_TUBE_CORAL, Block.DEAD_BRAIN_CORAL, Block.DEAD_BUBBLE_CORAL,
            Block.DEAD_FIRE_CORAL, Block.DEAD_HORN_CORAL
    );
    public static Collection<Key> CORAL_FAN = create(
            Block.TUBE_CORAL_FAN, Block.BRAIN_CORAL_FAN, Block.BUBBLE_CORAL_FAN,
            Block.FIRE_CORAL_FAN, Block.HORN_CORAL_FAN,
            Block.DEAD_TUBE_CORAL_FAN, Block.DEAD_BRAIN_CORAL_FAN, Block.DEAD_BUBBLE_CORAL_FAN,
            Block.DEAD_FIRE_CORAL_FAN, Block.DEAD_HORN_CORAL_FAN
    );
    public static final Collection<Key> GROWABLE = create( //todo for bonemeal
            Block.TORCHFLOWER_CROP, Block.MELON_STEM, Block.PUMPKIN_STEM,
            Block.WHEAT, Block.CARROTS, Block.NETHER_WART, Block.POTATOES,
            Block.PITCHER_CROP, Block.BEETROOTS, Block.COCOA
    );
    public static final Collection<Key> PRE_WATERLOGGED_BLOCKS = create(
            Block.KELP, Block.KELP_PLANT
    );
    public static final Collection<Key> CAULDRONS = builtin("minecraft:cauldrons");
    public static final Collection<Key> LANTERNS = extend(builtin("minecraft:lanterns"));
    public static final Collection<Key> CHAINS = extend(builtin("minecraft:chains"));
    public static final Collection<Key> BARS = extend(builtin("minecraft:bars"));
    public static final Collection<Key> LIGHTNING_RODS = extend(builtin("minecraft:lightning_rods"));
    public static final Collection<Key> COPPER_CHESTS = extend(builtin("minecraft:copper_chests"));
    public static final Collection<Key> UNRENDERABLE_DISPLAY_ENTITY_BLOCKS = group(
            SIGNS,
            ALL_HANGING_SIGNS,
            BANNERS,
            SKULLS,
            create(Block.BELL, Block.DECORATED_POT)
    );

    @SafeVarargs
    private static @NotNull Collection<Key> group(@NotNull Collection<Key>... tags) {
        var set = new HashSet<Key>();
        for (var tag : tags) {
            set.addAll(tag);
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<Key> extend(@NotNull Collection<Key> tag, @NotNull Block... block) {
        var set = new HashSet<>(tag);
        for (var b : block) {
            set.add(b.key());
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<Key> builtin(@NotNull String name) {
        var tag = Block.staticRegistry().getTag(Key.key(name));
        Check.notNull(tag, "Tag " + name + " is not registered");
        return StreamSupport.stream(tag.spliterator(), false).map(RegistryKey::key).toList();
    }

    private static @NotNull Collection<Key> create(@NotNull Block... block) {
        var set = new HashSet<Key>();
        for (var b : block) {
            set.add(b.key());
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<Key> create(@NotNull Supplier<Set<Block>> blocks) {
        var set = new HashSet<Key>();
        for (var b : blocks.get()) {
            set.add(b.key());
        }
        return Set.copyOf(set);
    }

    private static @NotNull Collection<Key> createFromProperty(@NotNull String property) {
        var set = new HashSet<Key>();
        for (var block : Block.values()) {
            if (block.getProperty(property) != null) {
                set.add(block.key());
            }
        }
        return Set.copyOf(set);
    }

}
