package net.hollowcube.map2.block;

import net.hollowcube.map2.block.placement.*;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.TerraformRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public final class PlacementRules {
    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();
    private static TerraformRegistry REGISTRY = null;

    public static void init(@NotNull Terraform tf) {
        REGISTRY = tf.registry();

        //
        // ==== WARNING ====
        // IT IS IMPORTANT TO RUN ALL BLOCKS THROUGH #register, EVEN IF THEY AREN'T TAGS
        // THAT CALL WILL ASSIGN DEFAULT BLOCK HANDLERS TO THE BLOCKS IF PRESENT
        //

        register(BlockTags.LOGS, AxisPlacementRule::new);
        register(Block.BAMBOO_BLOCK, AxisPlacementRule::new);
        register(Block.STRIPPED_BAMBOO_BLOCK, AxisPlacementRule::new);
        register(BlockTags.STAIRS, StairPlacementRule::new);
        register(BlockTags.SLABS, SlabPlacementRule::new);
        register(BlockTags.GLAZED_TERRACOTTA, b -> new FacingHorizontalPlacementRule(b, true));

        register(BlockTags.FENCES, FencePlacementRule::new);
        register(BlockTags.FENCE_GATES, FenceGatePlacementRule::new);
        register(BlockTags.WALLS, WallPlacementRule::new);
        register(BlockTags.GLASS_PANES, PanePlacementRule::new);
        register(Block.IRON_BARS, PanePlacementRule::new);

        register(BlockTags.DOORS, DoorPlacementRule::new);
        register(BlockTags.TRAPDOORS, TrapdoorPlacementRule::new);
        register(BlockTags.BUTTONS, ButtonPlacementRule::new);
        register(Block.LEVER, ButtonPlacementRule::new);
        register(BlockTags.BANNERS, BannerPlacementRule::new);
        register(BlockTags.STANDING_SIGNS, StandingSignPlacementRule::new);
        register(BlockTags.CEILING_HANGING_SIGNS, HangingSignPlacementRule::new); // For some reason it never calls wall hanging signs, we just have to convert to them in the placement rule
        register(BlockTags.BEDS, BedPlacementRule::new);
        register(BlockTags.ANVILS, AnvilPlacementRule::new);

        register(Block.FLOWER_POT, FlowerPotPlacementRule::new);
        register(BlockTags.POTTABLE_FLOWERS, SmallFlowerPlacementRule::new);
        register(BlockTags.TALL_FLOWERS, TallFlowerPlacementRule::new);
        register(Block.CHORUS_PLANT, ChorusPlantPlacementRule::new);
        register(Block.CAVE_VINES, CaveVinesPlacementRule::new);
        register(Block.TWISTING_VINES, TwistingVinesPlacementRule::new);
        register(Block.WEEPING_VINES, WeepingVinesPlacementRule::new);
        register(Block.VINE, b -> new VinePlacementRule(b, false));
        register(Block.GLOW_LICHEN, b -> new VinePlacementRule(b, true));
        register(Block.SCULK_VEIN, b -> new VinePlacementRule(b, true));
        register(Block.BIG_DRIPLEAF, BigDripleafPlacementRule::new);
        register(Block.SMALL_DRIPLEAF, SmallDripleafPlacementRule::new);

        register(Block.CHEST, ChestPlacementRule::new);
        register(Block.TRAPPED_CHEST, ChestPlacementRule::new);
        register(Block.ENDER_CHEST, b -> new FacingHorizontalPlacementRule(b, true));
        register(BlockTags.SHULKER_BOXES, b -> new ClickFacingPlacementRule(b, true, false));

        register(Block.TORCH, b -> new TorchPlacementRule(b, Block.WALL_TORCH));
        register(Block.SOUL_TORCH, b -> new TorchPlacementRule(b, Block.SOUL_WALL_TORCH));
        register(Block.REDSTONE_TORCH, b -> new TorchPlacementRule(b, Block.REDSTONE_WALL_TORCH));

        register(Block.BASALT, AxisPlacementRule::new);
        register(Block.POLISHED_BASALT, AxisPlacementRule::new);
        register(Block.BONE_BLOCK, AxisPlacementRule::new);
        register(Block.MUDDY_MANGROVE_ROOTS, AxisPlacementRule::new);
        register(Block.HAY_BLOCK, AxisPlacementRule::new);
        register(Block.PURPUR_PILLAR, AxisPlacementRule::new);
        register(Block.QUARTZ_PILLAR, AxisPlacementRule::new);
        register(Block.DEEPSLATE, AxisPlacementRule::new);
        register(Block.INFESTED_DEEPSLATE, AxisPlacementRule::new);
        register(Block.CHAIN, AxisPlacementRule::new);

        register(Block.FURNACE, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.LECTERN, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.JACK_O_LANTERN, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.CARVED_PUMPKIN, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.BEEHIVE, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.BEE_NEST, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.FURNACE, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.BLAST_FURNACE, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.STONECUTTER, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.LOOM, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.SMOKER, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.COMPARATOR, b -> new FacingHorizontalPlacementRule(b, true));
        register(Block.REPEATER, b -> new FacingHorizontalPlacementRule(b, true));

        register(Block.DISPENSER, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.DROPPER, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.PISTON, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.STICKY_PISTON, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.OBSERVER, b -> new FacingAllAxisPlacementRule(b, true));
        register(Block.COMMAND_BLOCK, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.CHAIN_COMMAND_BLOCK, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.REPEATING_COMMAND_BLOCK, b -> new FacingAllAxisPlacementRule(b, false));
        register(Block.CALIBRATED_SCULK_SENSOR, b -> new FacingHorizontalPlacementRule(b, false));

        register(Block.LIGHTNING_ROD, b -> new ClickFacePlacementRule(b, true));
        register(Block.END_ROD, b -> new ClickFacePlacementRule(b, true));

        register(Block.HOPPER, b -> new ClickFacingPlacementRule(b, false, true));
        register(Block.SMALL_AMETHYST_BUD, b -> new ClickFacingPlacementRule(b, true, false));
        register(Block.MEDIUM_AMETHYST_BUD, b -> new ClickFacingPlacementRule(b, true, false));
        register(Block.LARGE_AMETHYST_BUD, b -> new ClickFacingPlacementRule(b, true, false));
        register(Block.AMETHYST_CLUSTER, b -> new ClickFacingPlacementRule(b, true, false));

        register(BlockTags.CANDLES, b -> new BlockStackingPlacementRule(b, BlockStackingPlacementRule.CANDLE_PROPERTY));
        register(Block.SEA_PICKLE, b -> new BlockStackingPlacementRule(b, BlockStackingPlacementRule.SEA_PICKLE_PROPERTY));
        register(Block.TURTLE_EGG, b -> new BlockStackingPlacementRule(b, BlockStackingPlacementRule.TURTLE_EGGS_PROPERTY));

        register(Block.LANTERN, LanternPlacementRule::new);
        register(Block.SOUL_LANTERN, LanternPlacementRule::new);

        register(Block.LADDER, FacingClickHorizontalPlacementRule::new);

        register(Block.PLAYER_HEAD, HeadPlacementRule::new);
        register(BlockTags.SKULLS, HeadPlacementRule::new);

        register(Block.TRIPWIRE_HOOK, TripwireHookPlacementRule::new);
        register(Block.TRIPWIRE, TripwirePlacementRule::new);

        register(Block.DECORATED_POT, WaterloggedPlacementRule::new); //todo
        register(Block.SPAWNER, NoopPlacementRule::new);
        register(Block.END_PORTAL, NoopPlacementRule::new);

        register(Block.CONDUIT, WaterloggedPlacementRule::new);
        register(BlockTags.CORAL, WaterloggedPlacementRule::new);
        register(BlockTags.CORAL_FAN, CoralFanPlacementRule::new);

        // Annoying single use wall of shame >:(

        register(Block.BELL, BellPlacementRule::new);
        register(Block.GRINDSTONE, GrindstonePlacementRule::new);
        register(Block.POINTED_DRIPSTONE, DripstonePlacementRule::new);
        register(Block.SNOW, SnowPlacementRule::new);
        register(Block.SCAFFOLDING, ScaffoldingPlacementRule::new);
        register(Block.JIGSAW, JigsawPlacementRule::new);
        register(Block.FIRE, FirePlacementRule::new);

        //
        // ==== WARNING ====
        // IT IS IMPORTANT TO RUN ALL BLOCKS THROUGH #register, EVEN IF THEY AREN'T TAGS
        // THAT CALL WILL ASSIGN DEFAULT BLOCK HANDLERS TO THE BLOCKS IF PRESENT
        //

    }

    private static void register(@NotNull Collection<NamespaceID> tag, Function<Block, BlockPlacementRule> constructor) {
        for (var blockId : tag) {
            var ruleInstance = Objects.requireNonNull(constructor.apply(blockFromId(blockId)));
            BLOCK_MANAGER.registerBlockPlacementRule(ruleInstance);
        }
    }

    private static void register(@NotNull Block block, Function<Block, BlockPlacementRule> constructor) {
        var ruleInstance = Objects.requireNonNull(constructor.apply(blockFromId(block.namespace())));
        BLOCK_MANAGER.registerBlockPlacementRule(ruleInstance);
    }

    private static @NotNull Block blockFromId(@NotNull NamespaceID id) {
        // Convert block to block state using terraforms registry of handlers.
        return Objects.requireNonNull(REGISTRY.blockState(Objects.requireNonNull(Block.fromNamespaceId(id)).stateId()));
    }
}
