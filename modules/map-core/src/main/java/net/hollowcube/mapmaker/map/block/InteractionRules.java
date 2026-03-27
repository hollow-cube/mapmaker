package net.hollowcube.mapmaker.map.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.mapmaker.map.block.interaction.*;
import net.hollowcube.mapmaker.map.item.ItemTags;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public class InteractionRules {
    private static final Int2ObjectMap<BlockInteractionRule> blockRules = new Int2ObjectArrayMap<>();
    private static final Int2ObjectMap<BlockInteractionRule> itemRules = new Int2ObjectArrayMap<>();

    private static final Tag<Long> LAST_INTERACT_TICK = Tag.Long("last_interact_tick");

    static {
        block(Block.LECTERN, new LecternInteractionRule());
        block(Block.CAKE, new CakeInteractionRule());
        block(Block.DAYLIGHT_DETECTOR, new DaylightDetectorInteractionRule());
        block(BlockTags.TRAPDOORS, SimpleOpenableInteractionRule.INSTANCE);
        block(BlockTags.FENCE_GATES, SimpleOpenableInteractionRule.INSTANCE);
        block(Block.BARREL, SimpleOpenableInteractionRule.INSTANCE);
        block(BlockTags.DOORS, new DoorInteractionRule());
        block(BlockTags.ANY_WITH_LIT, new ToggleLitInteractionRule());
        block(Block.COMPOSTER, new ComposterInteractionRule());
        block(Block.PISTON, PistonInteractionRule.INSTANCE);
        block(Block.STICKY_PISTON, PistonInteractionRule.INSTANCE);
        block(Block.RESPAWN_ANCHOR, new RespawnAnchorInteractionRule());
        block(BlockTags.CANDLE_CAKES, new CandleCakeInteractionRule());
        block(Block.LEVER, new LeverInteractionRule());
        block(BlockTags.BUTTONS, new ButtonInteractionRule());
        block(Block.REPEATER, new RepeaterInteractionRule());
        block(Block.COMPARATOR, new ComparatorInteractionRule());
        block(Block.CHISELED_BOOKSHELF, ChiseledBookshelfInteractionRule.INSTANCE);
        block(Block.REDSTONE_WIRE, RedstoneWireInteractionRule.INSTANCE);
        block(BlockTags.COPPER_STATUES, CopperGolemInteractionRule.INSTANCE);

        item(Material.WATER_BUCKET, new WaterBucketInteractionRule());
        item(Material.LAVA_BUCKET, new LavaBucketInteractionRule());
        item(Material.POWDER_SNOW_BUCKET, new PowderSnowBucketInteractionRule());
        item(Material.BUCKET, new EmptyBucketInteractionRule());
        item(Material.FLINT_AND_STEEL, new FireInteractionRule());
        item(Material.FIRE_CHARGE, new FireInteractionRule());
        item(ItemTags.AXES, AxeInteractionRule.INSTANCE);
        item(ItemTags.SHOVELS, ShovelInteractionRule.INSTANCE);
        item(ItemTags.HOES, HoeInteractionRule.INSTANCE);
        item(Material.ITEM_FRAME, new ItemFrameInteractionRule(false));
        item(Material.GLOW_ITEM_FRAME, new ItemFrameInteractionRule(true));
        item(Material.PAINTING, new PaintingInteractionRule());
        item(Material.ENDER_EYE, new EnderEyeInteractionRule());
        item(Material.END_CRYSTAL, new EndCrystalInteractionRule());
        item(Material.ARMOR_STAND, new ArmorStandInteractionRule());
        item(Material.SCAFFOLDING, ScaffoldingInteractionRule.INSTANCE);
        item(Material.BONE_MEAL, new BonemealInteractionRule());
        item(Material.LILY_PAD, new LilyPadInteractionRule());
        item(Material.LEAD, LeadInteractionRule.INSTANCE);
    }

    public static void register(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(PlayerBlockInteractEvent.class, InteractionRules::handleBlockInteract);
        eventNode.addListener(PlayerBlockBreakEvent.class, InteractionRules::handleBlockBreak);
        eventNode.addListener(PlayerUseItemEvent.class, InteractionRules::handleItemUse);
    }

    // Handler functions

    private static void handleBlockInteract(@NotNull PlayerBlockInteractEvent event) {
        if (event.isCancelled() || event.isBlockingItemUse()) return;
        //todo how to block the other hand from interacting when we get this event _if_ we handled it in the main hand
        // for now just disable all interactions in off hand
        if (event.getHand() != PlayerHand.MAIN) return;

        var player = event.getPlayer();
        var itemStack = player.getItemInHand(event.getHand());

        // Try to apply a block rule first, if present
        var block = event.getBlock();
        var rule = blockRules.get(block.id());
        if (rule != null && rule.sneakState().test(player.isSneaking(), !itemStack.isAir())) {
            var interaction = new BlockInteractionRule.Interaction(
                    player, event.getInstance(), event.getBlockPosition(),
                    event.getBlockFace(), event.getCursorPosition(),
                    itemStack, event.getHand()
            );

            if (rule.handleInteraction(interaction)) {
                event.setBlockingItemUse(true);
                return;
            }

            // Rule was not applied, so continue to item rules
        }

        if (event.isBlockingItemUse()) return; // Skip item rules if the event blocks item use
        rule = itemRules.get(itemStack.material().id());
        if (rule == null || !rule.sneakState().test(player.isSneaking(), !itemStack.isAir())) return;

        var interaction = new BlockInteractionRule.Interaction(
                player, event.getInstance(), event.getBlockPosition(),
                event.getBlockFace(), event.getCursorPosition(),
                itemStack, event.getHand()
        );

        if (rule.handleInteraction(interaction)) {
            event.setBlockingItemUse(true);
        }
    }

    private static void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        var block = event.getBlock();
        if ("true".equals(block.getProperty("waterlogged")) || BlockTags.PRE_WATERLOGGED_BLOCKS.contains(block.key()))
            event.setResultBlock(Block.WATER);
    }

    private static void handleItemUse(@NotNull PlayerUseItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != PlayerHand.MAIN) return;

        var player = event.getPlayer();
        var itemStack = event.getItemStack();
        var rule = itemRules.get(itemStack.material().id());
        if (!(rule instanceof BlockInteractionRule.AirInteractionRule airRule)
                || !rule.sneakState().test(player.isSneaking(), !itemStack.isAir())) return;

        var interaction = new BlockInteractionRule.Interaction(
                player, event.getInstance(), null,
                null, null,
                itemStack, event.getHand()
        );

        if (airRule.handleAirInteraction(interaction)) {
            event.setCancelled(true);
        }
    }

    // Utility functions for registering the rules above

    private static void item(@NotNull Material material, @NotNull BlockInteractionRule rule) {
        itemRules.put(material.id(), rule);
    }

    private static void item(@NotNull Collection<Key> tag, @NotNull BlockInteractionRule rule) {
        tag.forEach(id -> item(Objects.requireNonNull(Material.fromKey(id)), rule));
    }

    private static void block(@NotNull Block block, @NotNull BlockInteractionRule rule) {
        blockRules.put(block.id(), rule);
    }

    private static void block(@NotNull Collection<Key> tag, @NotNull BlockInteractionRule rule) {
        tag.forEach(id -> block(Objects.requireNonNull(Block.fromKey(id)), rule));
    }

}
