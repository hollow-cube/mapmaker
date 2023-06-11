package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
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

        String state;
        int strippedState;
        if ("false".equals(block.getProperty("waterlogged")) && itemStack.material().equals(Material.WATER_BUCKET)) {
            block = block.withProperty("waterlogged", "true");
        } else if ("true".equals(block.getProperty("waterlogged")) && itemStack.material().equals(Material.BUCKET)) {
            block = block.withProperty("waterlogged", "false");
        } else if ((strippedState = STRIP_MAP.get(block.id())) != 0 && itemStack.material().name().contains("axe")) {
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

    static {
        STRIP_MAP = new Int2IntArrayMap();
        STRIP_MAP.put(Block.OAK_LOG.id(), Block.STRIPPED_OAK_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_OAK_LOG.id(), Block.OAK_LOG.id());

        CANDLE_CAKE_MAP = new Int2IntArrayMap();
        CANDLE_CAKE_MAP.put(Block.CANDLE.id(), Block.CANDLE_CAKE.id());
        CANDLE_CAKE_MAP.put(Block.BLACK_CANDLE.id(), Block.BLACK_CANDLE_CAKE.id());
    }
}
