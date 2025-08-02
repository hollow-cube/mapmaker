package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.FinishPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.feature.edit.item.DisplayEntityItem;
import net.hollowcube.mapmaker.map.feature.edit.item.EnterTestModeItem;
import net.hollowcube.mapmaker.map.feature.edit.item.SpawnPointItem;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BuilderMenuTabItems {

    private static final ItemCondition ALWAYS = (_, _) -> ItemVisibility.AVAILABLE;
    private static final ItemCondition DISABLED = (_, _) -> ItemVisibility.DISABLED;

    public static final Item[] CUSTOM_BLOCKS = new Item[]{
            Item.of(
                    FinishPlateBlock.ITEM,
                    "gui.builder_menu.custom_blocks.finish_plate",
                    "builder_menu/custom_blocks/finish_plate",
                    variant(MapVariant.PARKOUR)
            ),
            Item.of(
                    CheckpointPlateBlock.ITEM,
                    "gui.builder_menu.custom_blocks.checkpoint_plate",
                    "builder_menu/custom_blocks/checkpoint_plate",
                    variant(MapVariant.PARKOUR)
            ),
            Item.of(
                    StatusPlateBlock.ITEM,
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
                    EnterTestModeItem.INSTANCE,
//                    MapDetailsItem.INSTANCE,
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

    private static ItemCondition featureFlag(@NotNull FeatureFlag flag) {
        return (_, player) -> flag.test(player) ? ItemVisibility.AVAILABLE : ItemVisibility.HIDDEN;
    }

    private static ItemCondition variant(@NotNull MapVariant variant) {
        return (world, _) -> world.map().settings().getVariant() == variant ? ItemVisibility.AVAILABLE : ItemVisibility.DISABLED;
    }

    public record Item(
            @NotNull Consumer<Player> giver,
            @NotNull String translation,
            @NotNull Either<BadSprite, Material> icon,
            @NotNull ItemCondition condition
    ) {

        private static @NotNull Item of(@NotNull ItemHandler item, @NotNull String key, @NotNull String sprite, @NotNull ItemCondition condition) {
            return new Item(player -> giveCustomItem(player, item), key, Either.left(BadSprite.require(sprite)), condition);
        }

        private static @NotNull Item of(@NotNull Consumer<Player> giver, @NotNull String key, @NotNull Material icon, @NotNull ItemCondition condition) {
            return new Item(giver, key, Either.right(icon), condition);
        }

        public void give(@NotNull Player player) {
            if (!canGive(player)) return;
            this.giver.accept(player);
        }

        public boolean canGive(@NotNull Player player) {
            return condition.check(MapWorld.forPlayer(player), player) == ItemVisibility.AVAILABLE;
        }

        public boolean isVisible(@NotNull MapWorld world, @NotNull Player player) {
            return condition.check(world, player) != ItemVisibility.HIDDEN;
        }
    }

    private static void giveCustomItem(@NotNull Player player, @NotNull ItemHandler item) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        var itemStack = world.itemRegistry().getItemStack(item.key(), null);
        PlayerUtil.giveItem(player, itemStack);
        player.closeInventory();
    }

    @FunctionalInterface
    private interface ItemCondition {

        @NotNull ItemVisibility check(@NotNull MapWorld world, @NotNull Player player);
    }

    private enum ItemVisibility {
        AVAILABLE,
        HIDDEN,
        DISABLED,
    }
}
