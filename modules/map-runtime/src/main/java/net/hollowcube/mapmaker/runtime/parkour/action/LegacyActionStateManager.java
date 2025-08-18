package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditAttributeAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditLivesAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditTimerAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.attributes.ActionAttributes;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.attributes.AttributeMap;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItem;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class LegacyActionStateManager {
    private static final Equippable EMPTY_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            null, null, null, false, false, false, false,
            false, SoundEvent.ITEM_SHEARS_SNIP);
    private static final Equippable ELYTRA_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            "minecraft:elytra", null, null, false, false, false,
            false, false, SoundEvent.ITEM_SHEARS_SNIP);

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, LegacyActionStateManager::handleUpdatePlayerFromState)
            .addListener(ParkourMapPlayerStateUpdateEvent.class, LegacyActionStateManager::handleUpdateStateFromPlayer);

    private static void handleUpdatePlayerFromState(ParkourMapPlayerUpdateStateEvent event) {
        final var player = event.player();
        final var state = event.playState();

        // Update attributes
        var attributes = state.get(EditAttributeAction.SAVE_DATA, AttributeMap.EMPTY);
        for (var entry : ActionAttributes.ENTRIES.values()) {
            var attribute = entry.attribute();
            double value = attributes.getOrDefault(attribute, attribute.defaultValue());
            player.getAttribute(attribute).setBaseValue(value);
        }

        // Set the player health to the number of time they have (1 heart = 1 life)
        var lives = state.get(EditLivesAction.SAVE_DATA);
        if (lives != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2 * lives.max());
            player.setHealth(2 * lives.value());
        } else {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Attribute.MAX_HEALTH.defaultValue());
            player.setHealth((float) Attribute.MAX_HEALTH.defaultValue());
        }

        // Update the countdown timer (time may have been added)
        var timeLimit = state.get(EditTimerAction.SAVE_DATA);
        if (timeLimit != null && !event.isFreshState()) {
            player.setTag(EditTimerAction.COUNTDOWN_END, System.currentTimeMillis() + (timeLimit * 50));
        } else {
            player.removeTag(EditTimerAction.COUNTDOWN_END);
        }

        // Update the potions on the player
        player.clearEffects();
        for (var entry : state.get(Attachments.POTION_EFFECTS, new PotionEffectList()).entries()) {
            player.addEffect(new Potion(
                    entry.type().vanillaEffect(),
                    (byte) (entry.level() - 1),
                    entry.duration() <= 0 ? Potion.INFINITE_DURATION : entry.duration() / MinecraftServer.TICK_MS, // Convert from milliseconds to ticks
                    Potion.ICON_FLAG
            ));
        }

        var ghostBlocks = GhostBlockHolder.forPlayer(player);
        ghostBlocks.load(state.ghostBlocks());

        // Apply items to current state.
        var items = state.get(Attachments.HOTBAR_ITEMS, HotbarItems.EMPTY);
        player.getInventory().setItemStack(3, items.item0() == null ? ItemStack.AIR : items.item0().createItemStack());
        player.getInventory().setItemStack(4, items.item1() == null ? ItemStack.AIR : items.item1().createItemStack());
        player.getInventory().setItemStack(5, items.item2() == null ? ItemStack.AIR : items.item2().createItemStack());

        // Apply elytra or remove it if not relevant
        if (state.get(Attachments.ELYTRA, false)) {
            player.setChestplate(player.getChestplate().with(DataComponents.GLIDER)
                    .with(DataComponents.EQUIPPABLE, ELYTRA_EQUIPPABLE));
        } else {
            player.setChestplate(player.getChestplate().without(DataComponents.GLIDER)
                    .with(DataComponents.EQUIPPABLE, EMPTY_EQUIPPABLE));
            if (player.isFlyingWithElytra()) player.setFlyingWithElytra(false);
        }
    }

    private static void handleUpdateStateFromPlayer(ParkourMapPlayerStateUpdateEvent event) {
        final var player = event.player();
        final var playState = event.playState();
        final var now = System.currentTimeMillis();

        var countdownEnd = player.getTag(EditTimerAction.COUNTDOWN_END);
        if (countdownEnd != -1) {
            // We have to clamp it to 1 because if we don't when they rejoin their time limit will
            // be less than or equal to 0 meaning it will allow them to play forever due tp <= 0 being infinite time.
            playState.set(EditTimerAction.SAVE_DATA, (int) Math.max((countdownEnd - now) / 50, 1));
        }

        // Update remaining time for the remaining effects (and remove if expired)
        var iter = playState.get(Attachments.POTION_EFFECTS, new PotionEffectList()).entries().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.duration() <= 0) continue; // No need to update if infinite

            final TimedPotion activeEffect = player.getEffect(entry.type().vanillaEffect());
            if (activeEffect == null) {
                iter.remove(); // Expired effect
                continue;
            }

            // Otherwise, update the duration
            //todo convert all to ticks
            int remainingWallTime = (int) ((activeEffect.potion().duration() - (player.getAliveTicks() - activeEffect.startingTicks())) * 50);
            entry.setDuration(Math.max(0, remainingWallTime));
        }

        var ghostBlocks = GhostBlockHolder.forPlayerOptional(player);
        playState.setGhostBlocks(ghostBlocks == null ? Map.of() : ghostBlocks.save());

        var items = playState.get(Attachments.HOTBAR_ITEMS, HotbarItems.EMPTY);
        playState.set(Attachments.HOTBAR_ITEMS, new HotbarItems(
                OpUtils.map(items.item0(), item -> updateItemStack(player, item, 3)),
                OpUtils.map(items.item1(), item -> updateItemStack(player, item, 4)),
                OpUtils.map(items.item2(), item -> updateItemStack(player, item, 5))
        ));
    }

    private static @Nullable CheckpointItem updateItemStack(Player player, CheckpointItem item, int slot) {
        var itemStack = player.getInventory().getItemStack(slot);
        if (itemStack.isAir()) return null; // Consumed
        return item.updateFromItemStack(itemStack);
    }
}
