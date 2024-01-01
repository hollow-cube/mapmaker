package net.hollowcube.map.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.map.block.interaction.*;
import net.hollowcube.map.item.ItemTags;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public class InteractionRules {
    private static final Int2ObjectMap<BlockInteractionRule> blockRules = new Int2ObjectArrayMap<>();
    private static final Int2ObjectMap<BlockInteractionRule> itemRules = new Int2ObjectArrayMap<>();

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

        item(Material.WATER_BUCKET, new WaterBucketInteractionRule());
        item(Material.LAVA_BUCKET, new LavaBucketInteractionRule());
        item(Material.BUCKET, new EmptyBucketInteractionRule()); //TODO the client doesnt send a block interact for this. So this only handles unwaterlogging blocks, not removing water or lava blocks themselves
        item(Material.FLINT_AND_STEEL, new FireInteractRule());
        item(Material.FIRE_CHARGE, new FireInteractRule());
        item(ItemTags.AXES, new AxeInteractionRule());
        item(ItemTags.SHOVELS, new ShovelInteractionRule());
        item(ItemTags.HOES, new HoeInteractionRule());
        item(Material.ITEM_FRAME, new ItemFrameInteractionRule(false));
        item(Material.GLOW_ITEM_FRAME, new ItemFrameInteractionRule(true));
        item(Material.ENDER_EYE, new EnderEyeInteractionRule());
        item(Material.END_CRYSTAL, new EndCrystalInteractionRule());
    }

    public static void register(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(PlayerBlockInteractEvent.class, InteractionRules::handleBlockInteract);
        eventNode.addListener(PlayerBlockBreakEvent.class, InteractionRules::handleBlockBreak);
    }

    // Handler functions

    private static void handleBlockInteract(@NotNull PlayerBlockInteractEvent event) {
        //todo how to block the other hand from interacting when we get this event _if_ we handled it in the main hand
        // for now just disable all interactions in off hand
        if (event.getHand() != Player.Hand.MAIN) return;

        var player = event.getPlayer();
        var itemStack = player.getItemInHand(event.getHand());

        // Try to apply a block rule first, if present
        var block = event.getBlock();
        var rule = blockRules.get(block.id());
        if (rule != null && rule.sneakState().test(player.isSneaking(), !itemStack.isAir())) {
            var interaction = new BlockInteractionRule.Interaction(
                    player, event.getInstance(), event.getBlockPosition(),
                    event.getBlockFace(), itemStack, event.getHand()
            );

            if (rule.handleInteraction(interaction)) {
                event.setBlockingItemUse(true);
                return;
            }

            // Rule was not applied, so continue to item rules
        }

        rule = itemRules.get(itemStack.material().id());
        if (rule == null || !rule.sneakState().test(player.isSneaking(), !itemStack.isAir())) return;

        var interaction = new BlockInteractionRule.Interaction(
                player, event.getInstance(), event.getBlockPosition(),
                event.getBlockFace(), itemStack, event.getHand()
        );

        if (rule.handleInteraction(interaction)) {
            event.setBlockingItemUse(true);
        }
    }

    private static void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        var block = event.getBlock();
        if ("true".equals(block.getProperty("waterlogged")))
            event.setResultBlock(Block.WATER);
    }

    // Utility functions for registering the rules above

    private static void item(@NotNull Material material, @NotNull BlockInteractionRule rule) {
        itemRules.put(material.id(), rule);
    }

    private static void item(@NotNull Collection<NamespaceID> tag, @NotNull BlockInteractionRule rule) {
        tag.forEach(id -> item(Objects.requireNonNull(Material.fromNamespaceId(id)), rule));
    }

    private static void block(@NotNull Block block, @NotNull BlockInteractionRule rule) {
        blockRules.put(block.id(), rule);
    }

    private static void block(@NotNull Collection<NamespaceID> tag, @NotNull BlockInteractionRule rule) {
        tag.forEach(id -> block(Objects.requireNonNull(Block.fromNamespaceId(id)), rule));
    }

}
