package net.hollowcube.map.gui.hotbar;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO make Hotbar an interface on which hub/edit/play/test/etc implement
public final class EditMapHotbar {
    private EditMapHotbar() {
    }

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:map/editmaphotbar", EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, EditMapHotbar::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, EditMapHotbar::handleUseItemOnBlock);

    private static final int TEST_MODE_CMD = 508;

    private static final ItemStack TEST_MODE_ITEM = ItemStack.builder(Material.FEATHER)
            .displayName(Component.translatable("gui.map.hotbar.test_mode.name"))
            .lore(LanguageProviderV2.translateMulti("gui.map.hotbar.test_mode.lore", List.of()))
            .meta(meta -> meta.customModelData(TEST_MODE_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull Player player) {
        player.getInventory().setItemStack(8, TEST_MODE_ITEM);
    }

    private static void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData());
    }

    private static void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData());
    }

    private static void handleItem(@NotNull Player player, int customModelData) {
        var world = MapWorld.forPlayer(player);
        if (customModelData == TEST_MODE_CMD) {
            if (world instanceof TestingMapWorld testingWorld) {
                testingWorld.exitTestMode(player);
            }
        }
    }
}
