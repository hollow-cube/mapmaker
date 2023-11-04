package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.hollowcube.map.block.placement.BlockTags;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

@AutoService(FeatureProvider.class)
public class SpecialClickHandlingFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("special-click-handler", EventFilter.INSTANCE)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleUseItem)
            .addListener(PlayerBlockInteractEvent.class, this::handleShiftClick);
//            .addListener(PlayerBlockPlaceEvent.class, this::handlePlaceBlock);

    private static final Int2IntArrayMap STRIP_MAP;

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0) return false;

        world.addScopedEventNode(eventNode);
        return true;
    }

    private void handleUseItem(PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var instance = event.getInstance();
        var itemStack = event.getItemStack();
        var block = instance.getBlock(event.getPosition());

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
        } else if (block.id() == Block.CAKE.id()) {
            var bites = Integer.parseInt(block.getProperty("bites"));
            block = block.withProperty("bites", String.valueOf((bites + 1) % 7));
        } else if (block.id() == Block.DAYLIGHT_DETECTOR.id()) {
            var prop = Boolean.parseBoolean(block.getProperty("inverted"));
            block = block.withProperty("inverted", String.valueOf(!prop));
        } else if (block.id() == Block.LECTERN.id() && (itemStack.material().equals(Material.BOOK) || itemStack.material().equals(Material.WRITABLE_BOOK) || itemStack.material().equals(Material.WRITTEN_BOOK))) {
            //todo make those a set of materials
            block = block.withProperty("has_book", "true");
        } else if (itemStack.material().equals(Material.FLINT_AND_STEEL)) {
            var placePos = event.getPosition().relative(event.getBlockFace());

            var posBelow = placePos.add(0, -1, 0);
            if (instance.getBlock(posBelow, Block.Getter.Condition.TYPE).isSolid()) {
                // If solid block below, always place the fire on that
                instance.setBlock(placePos, Block.FIRE);
            } else {
                //todo more fire
            }
        } else if (itemStack.material().equals(Material.WATER_BUCKET)) {
            var waterPos = event.getPosition().relative(event.getBlockFace());
            if (instance.getBlock(waterPos, Block.Getter.Condition.TYPE).isAir())
                instance.setBlock(waterPos, Block.WATER);
            return;
        } else return;

        //todo the block update isnt being sent correctly here, this is a minestom bug
        event.getInstance().setBlock(event.getPosition(), block);
    }

    private void handleShiftClick(PlayerBlockInteractEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var instance = event.getInstance();
        var blockPosition = event.getBlockPosition();
        var block = event.getBlock();

        String state;
        if (event.getPlayer().isSneaking()) {
            if ((state = block.getProperty("lit")) != null) {
                block = block.withProperty("lit", state.equals("true") ? "false" : "true");
            } else if ((state = block.getProperty("extendable")) != null) {
                block = block.withProperty("extendable", state.equals("true") ? "false" : "true");
            } else if (block.id() == Block.FARMLAND.id()) {
                state = block.getProperty("moisture");
                block = block.withProperty("moisture", state.equals("7") ? "0" : "7");
            } else if (block.id() == Block.COMPOSTER.id()) {
                var level = Integer.parseInt(block.getProperty("level"));
                block = block.withProperty("level", String.valueOf((level + 1) % 9));
            } else if (block.id() == Block.PISTON.id() || block.id() == Block.STICKY_PISTON.id()) {
                var prop = Boolean.parseBoolean(block.getProperty("extended"));
                block = block.withProperty("extended", String.valueOf(!prop));
            } else if (BlockTags.MINECRAFT_SLABS.contains(block.namespace())) {
                var type = block.getProperty("type");
                block = block.withProperty("type", switch (type) {
                    case "bottom" -> "top";
                    case "top" -> "bottom";
                    default -> type;
                });
            } else if ((state = block.getProperty("has_book")) != null) {
                block = block.withProperty("has_book", state.equals("true") ? "false" : "true");
            } else return; // If we hit this then exit, otherwise we will update the block
        } else {
            if (BlockTags.MINECRAFT_TRAPDOORS.contains(block.namespace()) || BlockTags.MINECRAFT_FENCE_GATES.contains(block.namespace()) || block.id() == Block.BARREL.id()) {
                var open = Boolean.parseBoolean(block.getProperty("open"));
                block = block.withProperty("open", String.valueOf(!open));
            } else if (BlockTags.MINECRAFT_DOORS.contains(block.namespace())) {
                var open = Boolean.parseBoolean(block.getProperty("open"));

                var isTopHalf = block.getProperty("half").equalsIgnoreCase("upper");
                var otherPosition = blockPosition.add(0, isTopHalf ? -1 : 1, 0);
                var otherBlock = instance.getBlock(otherPosition);

                instance.setBlock(otherPosition, otherBlock.withProperty("open", String.valueOf(!open)));
                block = block.withProperty("open", String.valueOf(!open));
            } else if (block.id() == Block.PISTON.id() || block.id() == Block.STICKY_PISTON.id()) {
                var isExtended = Boolean.parseBoolean(block.getProperty("extended"));

                var facing = block.getProperty("facing");
                var otherPosition = blockPosition.relative(switch (facing) {
                    case "up" -> BlockFace.TOP;
                    case "down" -> BlockFace.BOTTOM;
                    case "north" -> BlockFace.NORTH;
                    case "south" -> BlockFace.SOUTH;
                    case "west" -> BlockFace.WEST;
                    case "east" -> BlockFace.EAST;
                    default -> throw new IllegalStateException("unreachable");
                });
                var otherBlock = instance.getBlock(otherPosition);

                if (isExtended) {
                    if (otherBlock.id() == Block.PISTON_HEAD.id() && otherBlock.getProperty("facing").equals(facing)) {
                        instance.setBlock(otherPosition, Block.AIR);
                        instance.playSound(Sound.sound(SoundEvent.BLOCK_PISTON_CONTRACT, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), blockPosition);
                    }
                } else {
                    if (otherBlock.id() != Block.AIR.id()) return;

                    instance.setBlock(otherPosition, Block.PISTON_HEAD.withProperty("facing", facing)
                            .withProperty("type", String.valueOf(block.id() == Block.STICKY_PISTON.id() ? "sticky" : "normal")));
                    instance.playSound(Sound.sound(SoundEvent.BLOCK_PISTON_EXTEND, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), blockPosition);
                }

                block = block.withProperty("extended", String.valueOf(!isExtended));
            } else return; // If we hit this then exit, otherwise we will update the block
        }

        // Update the block in the world to the new state
        event.setBlockingItemUse(true);
        instance.setBlock(event.getBlockPosition(), block);
    }

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
    }
}
