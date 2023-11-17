package net.hollowcube.map.block;

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
    public static final Collection<NamespaceID> FENCES = builtin("minecraft:fences");
    public static final Collection<NamespaceID> WOODEN_FENCES = builtin("minecraft:wooden_fences");
    public static final Collection<NamespaceID> FENCE_GATES = builtin("minecraft:fence_gates");
    public static final Collection<NamespaceID> ALL_SIGNS = builtin("minecraft:all_signs");
    public static final Collection<NamespaceID> WALL_SIGNS = builtin("minecraft:wall_signs");
    public static final Collection<NamespaceID> STANDING_SIGNS = builtin("minecraft:standing_signs");
    public static final Collection<NamespaceID> ANVILS = builtin("minecraft:anvil");
    public static final Collection<NamespaceID> TRAPDOORS = builtin("minecraft:trapdoors");
    public static final Collection<NamespaceID> CANDLES = builtin("minecraft:candles");
    public static final Collection<NamespaceID> BANNERS = builtin("minecraft:banners");
    public static final Collection<NamespaceID> DOORS = builtin("minecraft:doors");
    public static final Collection<NamespaceID> TERRACOTTA = builtin("minecraft:terracotta");
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
            Block.SUNFLOWER
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

}
