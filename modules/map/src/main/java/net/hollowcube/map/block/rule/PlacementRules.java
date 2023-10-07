package net.hollowcube.map.block.rule;

import net.hollowcube.map.block.handler.BannerBlockHandler;
import net.hollowcube.map.block.handler.ChestBlockHandler;
import net.hollowcube.map.block.handler.PlayerHeadBlockHandler;
import net.hollowcube.map.block.handler.SkullBlockHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.utils.NamespaceID;

import java.util.Objects;

public final class PlacementRules {

    public static void init() {
        BlockManager blockManager = MinecraftServer.getBlockManager();
//        blockManager.registerBlockPlacementRule(new RedstonePlacementRule());

        // Axis
        // Logs
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.OAK_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.SPRUCE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.BIRCH_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.JUNGLE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.ACACIA_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.DARK_OAK_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.MANGROVE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.CHERRY_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.CRIMSON_STEM));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.WARPED_STEM));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.BAMBOO_BLOCK));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.OAK_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.SPRUCE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.BIRCH_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.JUNGLE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.ACACIA_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.DARK_OAK_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.MANGROVE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.CHERRY_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.CRIMSON_HYPHAE));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.WARPED_HYPHAE));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_OAK_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_SPRUCE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_BIRCH_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_JUNGLE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_ACACIA_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_DARK_OAK_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_MANGROVE_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_CHERRY_LOG));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_CRIMSON_STEM));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_WARPED_STEM));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_BAMBOO_BLOCK));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_OAK_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_SPRUCE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_BIRCH_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_JUNGLE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_ACACIA_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_DARK_OAK_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_MANGROVE_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_CHERRY_WOOD));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_CRIMSON_HYPHAE));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.STRIPPED_WARPED_HYPHAE));
        // Everything else
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.BASALT));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.POLISHED_BASALT));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.BONE_BLOCK));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.MUDDY_MANGROVE_ROOTS));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.HAY_BLOCK));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.PURPUR_PILLAR));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.QUARTZ_PILLAR));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.DEEPSLATE));
        blockManager.registerBlockPlacementRule(new AxisPlacementRule(Block.CHAIN));

        // Facing
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.FURNACE, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.LECTERN, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.JACK_O_LANTERN, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.CARVED_PUMPKIN, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BEEHIVE, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BEE_NEST, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.FURNACE, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BLAST_FURNACE, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.STONECUTTER, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.LOOM, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.SMOKER, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.COMPARATOR, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.REPEATER, true));

        // All axis facing
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.DISPENSER));
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.DROPPER));
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.OBSERVER));
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.COMMAND_BLOCK));
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.CHAIN_COMMAND_BLOCK));
        blockManager.registerBlockPlacementRule(new FacingAllAxisPlacementRule(Block.REPEATING_COMMAND_BLOCK));

        // Bell
        blockManager.registerBlockPlacementRule(new BellPlacementRule());

        blockManager.registerBlockPlacementRule(new ClickFacePlacementRule(Block.LIGHTNING_ROD));
        blockManager.registerBlockPlacementRule(new ClickFacePlacementRule(Block.END_ROD, true));

        // Fences, Walls, and Gates
        for (var fenceGateId : BlockTags.MINECRAFT_FENCE_GATES.getValues()) {
            blockManager.registerBlockPlacementRule(new FenceGatePlacementRule(Block.fromNamespaceId(fenceGateId)));
        }
        for (var fenceId : BlockTags.FENCES.getValues()) {
            blockManager.registerBlockPlacementRule(new FencePlacementRule(Block.fromNamespaceId(fenceId)));
        }
        for (var wallId : BlockTags.WALLS.getValues()) {
            blockManager.registerBlockPlacementRule(new WallPlacementRule(Block.fromNamespaceId(wallId)));
        }

        // Facing, but based on which block you click
        blockManager.registerBlockPlacementRule(new ClickFacingPlacementRule(Block.HOPPER, false, true));
        blockManager.registerBlockPlacementRule(new ClickFacingPlacementRule(Block.SMALL_AMETHYST_BUD, true, false));
        blockManager.registerBlockPlacementRule(new ClickFacingPlacementRule(Block.MEDIUM_AMETHYST_BUD, true, false));
        blockManager.registerBlockPlacementRule(new ClickFacingPlacementRule(Block.LARGE_AMETHYST_BUD, true, false));
        blockManager.registerBlockPlacementRule(new ClickFacingPlacementRule(Block.AMETHYST_CLUSTER, true, false));


        // Chests
        blockManager.registerBlockPlacementRule(new ChestPlacementRule(Block.CHEST.withHandler(ChestBlockHandler.CHEST)));
        blockManager.registerBlockPlacementRule(new ChestPlacementRule(Block.TRAPPED_CHEST.withHandler(ChestBlockHandler.TRAPPED_CHEST)));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.ENDER_CHEST, true));

        // Stairs
        //todo completely broken
        for (NamespaceID id : BlockTags.MINECRAFT_STAIRS.getValues()) {
            blockManager.registerBlockPlacementRule(new StairPlacementRule(Block.fromNamespaceId(id)));
        }

        // Stacking
        for (var candleId : BlockTags.MINECRAFT_CANDLES.getValues()) {
            blockManager.registerBlockPlacementRule(new BlockStackingPlacementRule(
                    Block.fromNamespaceId(candleId), BlockStackingPlacementRule.CANDLE_PROPERTY));
        }
        blockManager.registerBlockPlacementRule(new BlockStackingPlacementRule(
                Block.SEA_PICKLE, BlockStackingPlacementRule.SEA_PICKLE_PROPERTY));

        // Lantern hanging
        blockManager.registerBlockPlacementRule(new LanternPlacementRule(Block.LANTERN));
        blockManager.registerBlockPlacementRule(new LanternPlacementRule(Block.SOUL_LANTERN));

        // Snow stacking
        blockManager.registerBlockPlacementRule(new SnowPlacementRule());

        // Ladder
        blockManager.registerBlockPlacementRule(new FacingClickHorizontalPlacementRule(Block.LADDER));

        // Banners
        //todo completely broken
        for (var bannerId : BlockTags.MINECRAFT_BANNERS.getValues()) {
            var bannerBlock = Objects.requireNonNull(Block.fromNamespaceId(bannerId))
                    .withHandler(BannerBlockHandler.INSTANCE);
            blockManager.registerBlockPlacementRule(new BannerPlacementRule(bannerBlock));
        }

        // Slabs
        for (var slabId : BlockTags.MINECRAFT_SLABS.getValues()) {
            blockManager.registerBlockPlacementRule(new SlabPlacementRule(Block.fromNamespaceId(slabId)));
        }

        // Head
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.PLAYER_HEAD.withHandler(PlayerHeadBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.SKELETON_SKULL.withHandler(SkullBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.WITHER_SKELETON_SKULL.withHandler(SkullBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.ZOMBIE_HEAD.withHandler(SkullBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.CREEPER_HEAD.withHandler(SkullBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.DRAGON_HEAD.withHandler(SkullBlockHandler.INSTANCE)));
        blockManager.registerBlockPlacementRule(new HeadPlacementRule(Block.PIGLIN_HEAD.withHandler(SkullBlockHandler.INSTANCE)));

        // Button
        for (var buttonId : BlockTags.MINECRAFT_BUTTONS.getValues()) {
            blockManager.registerBlockPlacementRule(new ButtonPlacementRule(Block.fromNamespaceId(buttonId)));
        }

        // Glass panes (there is no tag for some reason???), iron bars, and fences
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.WHITE_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.LIGHT_GRAY_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.GRAY_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.BLACK_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.BROWN_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.RED_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.ORANGE_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.YELLOW_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.LIME_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.GREEN_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.CYAN_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.LIGHT_BLUE_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.BLUE_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.PURPLE_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.MAGENTA_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.PINK_STAINED_GLASS_PANE));
        blockManager.registerBlockPlacementRule(new PanePlacementRule(Block.IRON_BARS));

        // Trapdoors
        blockManager.registerBlockPlacementRule(new TrapdoorPlacementRule(Block.IRON_TRAPDOOR));
        for (var trapdoorId : BlockTags.MINECRAFT_TRAPDOORS.getValues()) {
            blockManager.registerBlockPlacementRule(new TrapdoorPlacementRule(Block.fromNamespaceId(trapdoorId)));
        }

        // Doors
        for (var doorID : BlockTags.MINECRAFT_DOORS.getValues()) {
            blockManager.registerBlockPlacementRule(new DoorPlacementRule(Block.fromNamespaceId(doorID)));
        }

        // Beds
        for (var bedId : BlockTags.MINECRAFT_BEDS.getValues()) {
            blockManager.registerBlockPlacementRule(new BedPlacementRule(Block.fromNamespaceId(bedId)));
        }

        // Dripleaf
        blockManager.registerBlockPlacementRule(new BigDripleafPlacementRule());
        blockManager.registerBlockPlacementRule(new SmallDripleafPlacementRule());

        // Two block tall flowers
        blockManager.registerBlockPlacementRule(new TallFlowerPlacementRule(Block.TALL_GRASS));
        blockManager.registerBlockPlacementRule(new TallFlowerPlacementRule(Block.ROSE_BUSH));
        blockManager.registerBlockPlacementRule(new TallFlowerPlacementRule(Block.LILAC));
        blockManager.registerBlockPlacementRule(new TallFlowerPlacementRule(Block.SUNFLOWER));

        // Grindstone
        blockManager.registerBlockPlacementRule(new GrindstonePlacementRule());

        // Tripwires
        blockManager.registerBlockPlacementRule(new TripwireHookPlacementRule());
        blockManager.registerBlockPlacementRule(new TripwirePlacementRule());

        // Torches
        blockManager.registerBlockPlacementRule(new TorchPlacementRule(Block.TORCH, Block.WALL_TORCH));
        blockManager.registerBlockPlacementRule(new TorchPlacementRule(Block.SOUL_TORCH, Block.SOUL_WALL_TORCH));
        blockManager.registerBlockPlacementRule(new TorchPlacementRule(Block.REDSTONE_TORCH, Block.REDSTONE_WALL_TORCH));

        // Flower pot
        blockManager.registerBlockPlacementRule(new FlowerPotPlacementRule());
        for (var flowerId : BlockTags.SMALL_FLOWERS.getValues()) {
            blockManager.registerBlockPlacementRule(new SmallFlowerPlacementRule(Block.fromNamespaceId(flowerId)));
        }

        // Terracotta
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.WHITE_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.LIGHT_GRAY_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.GRAY_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BLACK_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BROWN_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.RED_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.ORANGE_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.YELLOW_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.LIME_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.GREEN_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.CYAN_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.LIGHT_BLUE_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.BLUE_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.PURPLE_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.MAGENTA_GLAZED_TERRACOTTA, true));
        blockManager.registerBlockPlacementRule(new FacingHorizontalPlacementRule(Block.PINK_GLAZED_TERRACOTTA, true));


        // Block entity
        for (var signId : BlockTags.MINECRAFT_STANDING_SIGNS.getValues()) {
            blockManager.registerBlockPlacementRule(new SignPlacementRule(Block.fromNamespaceId(signId)));
        }

        // Dripstone
        blockManager.registerBlockPlacementRule(new DripstonePlacementRule());

        // Cave vines (glow berries)
        blockManager.registerBlockPlacementRule(new CaveVinesPlacementRule());
    }
}
