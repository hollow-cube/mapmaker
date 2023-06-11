package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class SpecialClickHandlingFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("special-click-handler", EventFilter.INSTANCE)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleUseItem)
            .addListener(PlayerBlockInteractEvent.class, this::handleShiftClick);
    //.addListener(PlayerBlockPlaceEvent.class, this::handlePlaceBlock);

    private static final Int2IntArrayMap STRIP_MAP;
    private static final Int2IntArrayMap CANDLE_CAKE_MAP;

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0) return false;

        world.addScopedEventNode(eventNode);
        return true;
    }

    private void handleUseItem(PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var itemStack = event.getItemStack();
        var block = event.getInstance().getBlock(event.getPosition());

        int strippedState;
        if ("false".equals(block.getProperty("waterlogged")) && itemStack.material().equals(Material.WATER_BUCKET)) {
            block = block.withProperty("waterlogged", "true");
        } else if ("true".equals(block.getProperty("waterlogged")) && itemStack.material().equals(Material.BUCKET)) {
            block = block.withProperty("waterlogged", "false");
        } else if ((strippedState = STRIP_MAP.get(block.id())) != 0 && itemStack.material().equals(Material.WOODEN_AXE)
                || itemStack.material().equals(Material.STONE_AXE) || itemStack.material().equals(Material.IRON_AXE)
                || itemStack.material().equals(Material.GOLDEN_AXE) || itemStack.material().equals(Material.DIAMOND_AXE)
                || itemStack.material().equals(Material.NETHERITE_AXE)) {
            block = Block.fromBlockId(strippedState).withProperties(block.properties());
        } else return;

        //todo the block update isnt being sent correctly here, this is a minestom bug
        event.getInstance().setBlock(event.getPosition(), block);
    }

    private void handleShiftClick(PlayerBlockInteractEvent event) {
        if (event.getHand() != Player.Hand.MAIN || !event.getPlayer().isSneaking()) return;

        //todo waterlogging shouldnt be on shift click, handle with a different eventr
//        var item = event.getPlayer().getItemInHand(event.getHand());

        var block = event.getBlock();

        String state;
        if ((state = block.getProperty("lit")) != null) {
            block = block.withProperty("lit", state.equals("true") ? "false" : "true");
        } else if ((state = block.getProperty("extendable")) != null) {
            block = block.withProperty("extendable", state.equals("true") ? "false" : "true");
        } else if (block.id() == Block.FARMLAND.id()) {
            state = block.getProperty("moisture");
            block = block.withProperty("moisture", state.equals("7") ? "0" : "7");
        } else if (block.name().contains("trapdoor") || block.id() == Block.BARREL.id()) {
            // todo should use trapdoor block tag i guess
            state = block.getProperty("open");
            block = block.withProperty("open", state.equals("true") ? "false" : "true");
        } else if (block.id() == Block.CAKE.id()) {
            var bites = Integer.parseInt(block.getProperty("bites"));
            block = block.withProperty("bites", String.valueOf((bites + 1) % 7));
        } else if (block.id() == Block.COMPOSTER.id()) {
            var level = Integer.parseInt(block.getProperty("level"));
            block = block.withProperty("level", String.valueOf((level + 1) % 9));
        } else return; // If we hit this then exit, otherwise we will update the block

        // Update the block in the world to the new state
        event.getInstance().setBlock(event.getBlockPosition(), block);
    }

//    //TODO fix more blocks being placed on top and sides of cake
//    private void handlePlaceBlock(PlayerBlockPlaceEvent event) {
//        var block = event.getBlock();
//        System.out.println("handlePlaceBlock with block " + block.name());
//        if (block == Block.CAKE || block == Block.CANDLE) {
//            System.out.println("placed cake or candle at " + event.getBlockPosition());
//        }
//        var isCakeUnder = event.getInstance().getBlock(event.getBlockPosition().sub(0, 1, 0)) == Block.CAKE;
//        System.out.println("isCakeUnder " + isCakeUnder);
//        int cakeCandleState;
//        if ((cakeCandleState = CANDLE_CAKE_MAP.get(block.id())) !=0 && CANDLE_CAKE_MAP.containsKey(block.id())) {
//            System.out.println("HEHE1");
//            block = Block.fromBlockId(cakeCandleState);
//            System.out.println("HEHE2");
//        } else return;
//
//        //todo the block update isnt being sent correctly here, this is a minestom bug
//        event.getInstance().setBlock(event.getBlockPosition().sub(0, 1, 0), block);
//        System.out.println("now setting WAAA" + block.name());
//    }

    static {
        STRIP_MAP = new Int2IntArrayMap();
        // Log Types
        STRIP_MAP.put(Block.OAK_LOG.id(), Block.STRIPPED_OAK_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_OAK_LOG.id(), Block.OAK_LOG.id());

        STRIP_MAP.put(Block.SPRUCE_LOG.id(), Block.STRIPPED_SPRUCE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_SPRUCE_LOG.id(), Block.SPRUCE_LOG.id());

        STRIP_MAP.put(Block.BIRCH_LOG.id(), Block.STRIPPED_BIRCH_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_BIRCH_LOG.id(), Block.BIRCH_LOG.id());

        STRIP_MAP.put(Block.JUNGLE_LOG.id(), Block.STRIPPED_JUNGLE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_JUNGLE_LOG.id(), Block.JUNGLE_LOG.id());

        STRIP_MAP.put(Block.ACACIA_LOG.id(), Block.STRIPPED_ACACIA_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_ACACIA_LOG.id(), Block.ACACIA_LOG.id());

        STRIP_MAP.put(Block.DARK_OAK_LOG.id(), Block.STRIPPED_DARK_OAK_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_DARK_OAK_LOG.id(), Block.DARK_OAK_LOG.id());

        STRIP_MAP.put(Block.MANGROVE_LOG.id(), Block.STRIPPED_MANGROVE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_MANGROVE_LOG.id(), Block.MANGROVE_LOG.id());

        STRIP_MAP.put(Block.CHERRY_LOG.id(), Block.STRIPPED_CHERRY_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_CHERRY_LOG.id(), Block.CHERRY_LOG.id());

        STRIP_MAP.put(Block.WARPED_STEM.id(), Block.STRIPPED_WARPED_STEM.id());
        STRIP_MAP.put(Block.STRIPPED_WARPED_STEM.id(), Block.WARPED_STEM.id());

        STRIP_MAP.put(Block.CRIMSON_STEM.id(), Block.STRIPPED_CRIMSON_STEM.id());
        STRIP_MAP.put(Block.STRIPPED_CRIMSON_STEM.id(), Block.CRIMSON_STEM.id());

        // Wood Types
        STRIP_MAP.put(Block.OAK_WOOD.id(), Block.STRIPPED_OAK_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_OAK_WOOD.id(), Block.OAK_WOOD.id());

        STRIP_MAP.put(Block.SPRUCE_WOOD.id(), Block.STRIPPED_SPRUCE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_SPRUCE_WOOD.id(), Block.SPRUCE_WOOD.id());

        STRIP_MAP.put(Block.BIRCH_WOOD.id(), Block.STRIPPED_BIRCH_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_BIRCH_WOOD.id(), Block.BIRCH_WOOD.id());

        STRIP_MAP.put(Block.JUNGLE_WOOD.id(), Block.STRIPPED_JUNGLE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_JUNGLE_WOOD.id(), Block.JUNGLE_WOOD.id());

        STRIP_MAP.put(Block.ACACIA_WOOD.id(), Block.STRIPPED_ACACIA_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_ACACIA_WOOD.id(), Block.ACACIA_WOOD.id());

        STRIP_MAP.put(Block.DARK_OAK_WOOD.id(), Block.STRIPPED_DARK_OAK_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_DARK_OAK_WOOD.id(), Block.DARK_OAK_WOOD.id());

        STRIP_MAP.put(Block.MANGROVE_WOOD.id(), Block.STRIPPED_MANGROVE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_MANGROVE_WOOD.id(), Block.MANGROVE_WOOD.id());

        STRIP_MAP.put(Block.CHERRY_WOOD.id(), Block.STRIPPED_CHERRY_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_CHERRY_WOOD.id(), Block.CHERRY_WOOD.id());

        STRIP_MAP.put(Block.WARPED_HYPHAE.id(), Block.STRIPPED_WARPED_HYPHAE.id());
        STRIP_MAP.put(Block.STRIPPED_WARPED_HYPHAE.id(), Block.WARPED_HYPHAE.id());

        STRIP_MAP.put(Block.CRIMSON_HYPHAE.id(), Block.STRIPPED_CRIMSON_HYPHAE.id());
        STRIP_MAP.put(Block.STRIPPED_CRIMSON_HYPHAE.id(), Block.CRIMSON_HYPHAE.id());



        // Candle Cake Types
        CANDLE_CAKE_MAP = new Int2IntArrayMap();
        CANDLE_CAKE_MAP.put(Block.CANDLE.id(), Block.CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.WHITE_CANDLE.id(), Block.WHITE_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.LIGHT_GRAY_CANDLE.id(), Block.LIGHT_GRAY_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.GRAY_CANDLE.id(), Block.GRAY_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.BLACK_CANDLE.id(), Block.BLACK_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.BROWN_CANDLE.id(), Block.BROWN_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.RED_CANDLE.id(), Block.RED_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.ORANGE_CANDLE.id(), Block.ORANGE_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.YELLOW_CANDLE.id(), Block.YELLOW_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.LIME_CANDLE.id(), Block.LIME_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.GREEN_CANDLE.id(), Block.GREEN_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.CYAN_CANDLE.id(), Block.CYAN_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.LIGHT_BLUE_CANDLE.id(), Block.LIGHT_BLUE_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.BLUE_CANDLE.id(), Block.BLUE_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.PURPLE_CANDLE.id(), Block.PURPLE_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.MAGENTA_CANDLE.id(), Block.MAGENTA_CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.PINK_CANDLE.id(), Block.PINK_CANDLE_CAKE.id());
    }
}
