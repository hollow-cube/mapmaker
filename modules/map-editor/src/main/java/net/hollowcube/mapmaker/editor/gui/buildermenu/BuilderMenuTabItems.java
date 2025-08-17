package net.hollowcube.mapmaker.editor.gui.buildermenu;

import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.editor.item.DisplayEntityItem;
import net.hollowcube.mapmaker.editor.item.EnterTestModeItem;
import net.hollowcube.mapmaker.editor.item.SpawnPointItem;
import net.hollowcube.mapmaker.editor.parkour.CheckpointEditor;
import net.hollowcube.mapmaker.editor.parkour.FinishEditor;
import net.hollowcube.mapmaker.editor.parkour.StatusEditor;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.function.Consumer;

public class BuilderMenuTabItems {

    private static final ItemCondition ALWAYS = (_, _) -> ItemVisibility.AVAILABLE;
    private static final ItemCondition DISABLED = (_, _) -> ItemVisibility.DISABLED;

    public static final Item[] CUSTOM_BLOCKS = new Item[]{
            Item.of(
                    FinishEditor.PLATE_ITEM,
                    "gui.builder_menu.custom_blocks.finish_plate",
                    "builder_menu/custom_blocks/finish_plate",
                    variant(MapVariant.PARKOUR)
            ),
            Item.of(
                    CheckpointEditor.PLATE_ITEM,
                    "gui.builder_menu.custom_blocks.checkpoint_plate",
                    "builder_menu/custom_blocks/checkpoint_plate",
                    variant(MapVariant.PARKOUR)
            ),
            Item.of(
                    StatusEditor.PLATE_ITEM,
                    "gui.builder_menu.custom_blocks.status_plate",
                    "builder_menu/custom_blocks/status_plate",
                    variant(MapVariant.PARKOUR)
            )
    };

    public static final Item[] BUILD_TOOLS = new Item[]{
            Item.of(
                    player -> PlayerUtil.giveItem(
                            player,
                            ItemStack.of(Material.DEBUG_STICK)
                    ),
                    "gui.builder_menu.build_tools.debug_stick",
                    Material.DEBUG_STICK,
                    ALWAYS
            ),
            Item.of(
                    player -> PlayerUtil.giveItem(
                            player,
                            LocalSession.forPlayer(player).terraform().toolHandler().createBuiltinTool("terraform:wand")
                    ),
                    "gui.builder_menu.build_tools.wand",
                    Material.WOODEN_AXE,
                    ALWAYS
            )
    };

    public static final Item[] CUSTOM_ITEMS = new Item[]{
            Item.of(
                    MapDetailsItem.INSTANCE,
                    "gui.builder_menu.custom_items.edit_map_details",
                    "hud/hotbar/edit_map_details",
                    DISABLED
            ),
            Item.of(
                    EnterTestModeItem.INSTANCE,
                    "gui.builder_menu.custom_items.enter_testing_mode",
                    "hud/hotbar/enter_test_mode",
                    variant(MapVariant.PARKOUR)
            ),
            Item.of(
                    SpawnPointItem.INSTANCE,
                    "gui.builder_menu.custom_items.spawn_point",
                    "hud/hotbar/spawn_point",
                    ALWAYS
            ),
            Item.of(
                    DisplayEntityItem.INSTANCE,
                    "gui.builder_menu.custom_items.display_entity",
                    "hud/hotbar/cosmetic_menu",
                    featureFlag(MapFeatureFlags.DISPLAY_ENTITY_EDITOR)
            )
    };

    private static ItemCondition featureFlag(FeatureFlag flag) {
        return (_, player) -> flag.test(player) ? ItemVisibility.AVAILABLE : ItemVisibility.HIDDEN;
    }

    private static ItemCondition variant(MapVariant variant) {
        return (world, _) -> world.map().settings().getVariant() == variant ? ItemVisibility.AVAILABLE : ItemVisibility.DISABLED;
    }

    public record Item(
            Consumer<Player> giver,
            String translation,
            Either<BadSprite, Material> icon,
            ItemCondition condition
    ) {

        private static Item of(ItemHandler item, String key, String sprite, ItemCondition condition) {
            return new Item(player -> giveCustomItem(player, item), key, Either.left(BadSprite.require(sprite)), condition);
        }

        private static Item of(Consumer<Player> giver, String key, Material icon, ItemCondition condition) {
            return new Item(giver, key, Either.right(icon), condition);
        }

        public void give(Player player) {
            if (!canGive(player)) return;
            this.giver.accept(player);
        }

        public boolean canGive(Player player) {
            var world = MapWorld.forPlayer(player);
            return world != null && condition.check(world, player) == ItemVisibility.AVAILABLE;
        }

        public boolean isVisible(MapWorld world, Player player) {
            return condition.check(world, player) != ItemVisibility.HIDDEN;
        }
    }

    private static void giveCustomItem(Player player, ItemHandler item) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof EditorState.Building)) return;

        var itemStack = world.itemRegistry().getItemStack(item.key(), null);
        PlayerUtil.giveItem(player, itemStack);
        player.closeInventory();
    }

    @FunctionalInterface
    private interface ItemCondition {

        ItemVisibility check(MapWorld world, Player player);
    }

    private enum ItemVisibility {
        AVAILABLE,
        HIDDEN,
        DISABLED,
    }
}
