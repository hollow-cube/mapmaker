package net.hollowcube.mapmaker.map.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryClickEvent;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class CustomizableHotbarManager {
    private static final Tag<CustomizableHotbarManager> TAG = Tag.Transient("active_hotbar");
    private static final int N_ENTRIES = 9;

    public static final ItemHandler RESET_TO_DEFAULT_ITEM = new ResetToDefaultItem();

    public static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    public static boolean isActive(@NotNull Player player) {
        return player.hasTag(TAG);
    }

    public static void unregister(@NotNull Player player) {
        player.removeTag(TAG);
    }

    private final PlayerSetting<String[]> setting; // Empty string or null == not present.
    private final String[] defaultPositions;
    private final Map<String, BiPredicate<Player, MapWorld>> conditions;

    private CustomizableHotbarManager(@NotNull String name, @Nullable String @NotNull [] defaultPositions, @NotNull Map<String, BiPredicate<Player, MapWorld>> conditions) {
        setting = PlayerSetting.create(name, new String[0], this::writeHotbarSetting, this::readHotbarSetting);
        this.defaultPositions = defaultPositions;
        this.conditions = conditions;
    }

    public void registerEvents(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(InventoryPreClickEvent.class, this::handlePreClick);
        eventNode.addListener(InventoryClickEvent.class, this::handlePostClick);
        eventNode.addListener(InventoryCloseEvent.class, this::handleCloseInventory);
    }

    public void apply(@NotNull Player player, @NotNull MapWorld world) {
        var itemRegistry = world.itemRegistry();
        var inventory = player.getInventory();

        var customPositions = PlayerDataV2.fromPlayer(player).getSetting(setting);
        for (int i = 0; i < 9; i++) {
            var itemId = i < customPositions.length ? customPositions[i] : defaultPositions[i];
            if (itemId == null || itemId.isEmpty()) {
                inventory.setItemStack(i, ItemStack.AIR);
                continue;
            }

            var condition = conditions.get(itemId);
            if (condition != null && !condition.test(player, world)) {
                inventory.setItemStack(i, ItemStack.AIR);
                continue;
            }

            inventory.setItemStack(i, itemRegistry.getItemStack(itemId, null));
        }
        // Always add the reset item (bottom right of inventory)
        inventory.setItemStack(35, itemRegistry.getItemStack(ResetToDefaultItem.ID, null));

        player.setTag(TAG, this);
    }

    private @NotNull JsonElement writeHotbarSetting(String[] hotbar) {
        var array = new JsonArray();
        for (int i = 0; i < N_ENTRIES; i++) {
            // If the size is less than max, fill it in with the defaults. This should only
            // happen when setting your inventory back to the default.
            var item = i < hotbar.length ? hotbar[i] : defaultPositions[i];
            array.add(item == null ? JsonNull.INSTANCE : new JsonPrimitive(item));
        }
        return array;
    }

    private String[] readHotbarSetting(@NotNull JsonElement elem) {
        var array = elem.getAsJsonArray();
        var hotbar = new String[N_ENTRIES];
        for (int i = 0; i < Math.min(array.size(), N_ENTRIES); i++) {
            var item = array.get(i);
            if (!item.isJsonNull()) hotbar[i] = item.getAsString();
        }
        return hotbar;
    }

    private void handlePreClick(@NotNull InventoryPreClickEvent event) {
        var player = event.getPlayer();
        if (!MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) return; // Not enabled for this player
        if (this != player.getTag(TAG) || event.getInventory() != null || player.getOpenInventory() != null)
            return; // Not active on this hotbar or not the player inventory

        // TODO(1.21.4)
//        if (event.getSlot() < 0 || event.getSlot() > 8 || event.getClickType() != ClickType.LEFT_CLICK) {
//            event.setCancelled(true);
//            return; // Not the hotbar or not a left click
//        }

        // Otherwise we allow the change, and will save the hotbar on the post event.
        // Do it during post so that any other cancellation will be respected
    }

    private void handlePostClick(@NotNull InventoryClickEvent event) {
        var player = event.getPlayer();
        if (!MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) return; // Not enabled for this player
        if (this != player.getTag(TAG) || event.getInventory() != null || player.getOpenInventory() != null)
            return; // Not active on this hotbar or not the player inventory
        if (event.getSlot() < 0 || event.getSlot() > 8 || event.getClickType() != ClickType.LEFT_CLICK)
            return; // Not the hotbar or not a left click

        // Don't do anything unless clicking air (meaning there will be no cursor result)
        // We shouldnt save the intermediate states, only once fully changed.
        if (!event.getClickedItem().isAir())
            return;

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity
        var itemRegistry = world.itemRegistry();

        // Save the changes
        var newHotbar = new String[N_ENTRIES];
        for (int i = 0; i < N_ENTRIES; i++) {
            var item = player.getInventory().getItemStack(i);
            if (item.isAir()) continue;
            newHotbar[i] = itemRegistry.getItemId(item);
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        // If they set it equal to the default, wipe the saved data. This will allow future changes to the default
        // to be applied to this player, rather than being stuck with their current state.
        if (Arrays.equals(defaultPositions, newHotbar))
            newHotbar = new String[0];
        if (Arrays.equals(playerData.getSetting(setting), newHotbar))
            return; // No change
        playerData.setSetting(setting, newHotbar);
    }

    private void handleCloseInventory(@NotNull InventoryCloseEvent event) {
        var player = event.getPlayer();
        if (!MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) return; // Not enabled for this player
        if (this != player.getTag(TAG) || event.getInventory() != null)
            return; // Not active on this hotbar or not the player inventory
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        // Ensure the inventory is up-to-date with their configuration.
        // This also solves the case where they close their inventory with an item on their cursor.
        event.getPlayer().getInventory().setCursorItem(ItemStack.AIR);
        apply(player, world);

        // Also write player data updates.
        FutureUtil.submitVirtual(() -> PlayerDataV2.fromPlayer(player)
                .writeUpdatesUpstream(world.server().playerService()));
    }

    public static class Builder {
        private final String name;

        private final String[] defaultPositions = new String[N_ENTRIES];
        private final Map<String, BiPredicate<Player, MapWorld>> conditions = new HashMap<>();

        private Builder(@NotNull String name) {
            this.name = name;
        }

        public @NotNull Builder defaultItem(int slot, @NotNull String itemId) {
            return defaultItem(slot, itemId, null);
        }

        public @NotNull Builder defaultItem(int slot, @NotNull String itemId, @Nullable BiPredicate<Player, MapWorld> condition) {
            Check.argCondition(slot < 0 || slot >= N_ENTRIES, "Slot out of range");
            defaultPositions[slot] = itemId;
            if (condition != null) {
                conditions.put(itemId, condition);
            }
            return this;
        }

        public @NotNull CustomizableHotbarManager build() {
            return new CustomizableHotbarManager(name, defaultPositions, conditions);
        }
    }

    private static class ResetToDefaultItem extends ItemHandler {
        private static final BadSprite SPRITE = BadSprite.require("hammer"); // TODO(1.21.4)
        public static final String ID = "mapmaker:reset_hotbar";

        public ResetToDefaultItem() {
            super(ID, LEFT_CLICK_GUI);
        }

        @Override
        public @Nullable BadSprite sprite() {
            return SPRITE;
        }

        @Override
        protected void leftClicked(@NotNull Click click) {
            var hotbar = click.player().getTag(TAG);
            if (hotbar == null) return;

            var world = MapWorld.forPlayerOptional(click.player());
            if (world == null) return; // Sanity

            var playerData = PlayerDataV2.fromPlayer(click.player());
            playerData.setSetting(hotbar.setting, new String[0]);
            hotbar.apply(click.player(), world);

            // Save the reset setting.
            FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(world.server().playerService()));
        }
    }
}
