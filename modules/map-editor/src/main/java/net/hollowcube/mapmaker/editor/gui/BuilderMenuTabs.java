package net.hollowcube.mapmaker.editor.gui;

import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.editor.item.EnterTestModeItem;
import net.hollowcube.mapmaker.editor.item.SpawnPointItem;
import net.hollowcube.mapmaker.editor.parkour.CheckpointEditor;
import net.hollowcube.mapmaker.editor.parkour.FinishEditor;
import net.hollowcube.mapmaker.editor.parkour.StatusEditor;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Locale;
import java.util.function.Consumer;

public enum BuilderMenuTabs {
    CUSTOM_BLOCKS(
        "icon2/1_1/box",
        Item.of(
            FinishEditor.PLATE_ITEM,
            "gui.builder_menu.custom_blocks.finish_plate",
            "builder_menu/finish_plate"
        ),
        Item.of(
            CheckpointEditor.PLATE_ITEM,
            "gui.builder_menu.custom_blocks.checkpoint_plate",
            "builder_menu/checkpoint_plate"
        ),
        Item.of(
            StatusEditor.PLATE_ITEM,
            "gui.builder_menu.custom_blocks.status_plate",
            "builder_menu/status_plate"
        )
    ),
    BUILD_TOOLS(
        "icon2/1_1/hammer",
        Item.of(
            player -> PlayerUtil.giveItem(
                player,
                ItemStack.of(Material.DEBUG_STICK)
            ),
            "gui.builder_menu.build_tools.debug_stick",
            Material.DEBUG_STICK
        ),
        Item.of(
            player -> PlayerUtil.giveItem(
                player,
                LocalSession.forPlayer(player).terraform().toolHandler().createBuiltinTool("terraform:wand")
            ),
            "gui.builder_menu.build_tools.wand",
            Material.WOODEN_AXE
        )
    ),
    CUSTOM_ITEMS(
        "icon2/1_1/screwdriver",
        Item.of(
            EnterTestModeItem.INSTANCE,
            "gui.builder_menu.custom_items.enter_testing_mode",
            "hud/hotbar/enter_test_mode"
        ),
        Item.of(
            SpawnPointItem.INSTANCE,
            "gui.builder_menu.custom_items.spawn_point",
            "hud/hotbar/spawn_point"
        )
    );

    private final String sprite;
    private final Item[] items;

    BuilderMenuTabs(String sprite, Item... items) {
        this.sprite = sprite;
        this.items = items;
    }

    public String translation() {
        return "gui.builder_menu.tab." + this.name().toLowerCase(Locale.ROOT);
    }

    public String icon() {
        return this.sprite;
    }

    public Item[] items() {
        return items;
    }

    public record Item(
        Consumer<Player> giver,
        String translation,
        ItemStack icon
    ) {

        private static Item of(ItemHandler item, String key, String sprite) {
            return new Item(player -> giveCustomItem(player, item), key, ItemStack.of(Material.STICK).with(
                DataComponents.ITEM_MODEL, BadSprite.require(sprite).model()
            ));
        }

        private static Item of(Consumer<Player> giver, String key, Material icon) {
            return new Item(giver, key, ItemStack.of(icon));
        }

        public void give(Player player) {
            this.giver.accept(player);
        }
    }

    private static void giveCustomItem(Player player, ItemHandler item) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof EditorState.Building)) return;

        var itemStack = world.itemRegistry().getItemStack(item.key(), null);
        PlayerUtil.giveItem(player, itemStack);
        player.closeInventory();
    }
}
