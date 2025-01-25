package net.hollowcube.mapmaker.map.gui.buildermenu;

import com.mojang.datafixers.util.Either;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.FinishPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.feature.edit.item.EnterTestModeItem;
import net.hollowcube.mapmaker.map.feature.edit.item.SpawnPointItem;
import net.hollowcube.mapmaker.map.feature.play.item.MapDetailsItem;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.PlayerUtil;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BuilderMenuTabItems {

    private static final ItemCondition ALWAYS = (world, player) -> TriState.TRUE;
    private static final ItemCondition DISABLED = (world, player) -> TriState.NOT_SET;
    private static final ItemCondition PARKOUR_ONLY_VISIBLE = variant(MapVariant.PARKOUR);

    public static final Item[] CUSTOM_BLOCKS = new Item[]{
            Item.of(
                    FinishPlateBlock.ITEM,
                    "gui.builder_menu.custom_blocks.finish_plate",
                    "builder_menu/custom_blocks/finish_plate",
                    PARKOUR_ONLY_VISIBLE
            ),
            Item.of(
                    CheckpointPlateBlock.ITEM,
                    "gui.builder_menu.custom_blocks.checkpoint_plate",
                    "builder_menu/custom_blocks/checkpoint_plate",
                    PARKOUR_ONLY_VISIBLE
            ),
            Item.of(
                    StatusPlateBlock.ITEM,
                    "gui.builder_menu.custom_blocks.status_plate",
                    "builder_menu/custom_blocks/status_plate",
                    PARKOUR_ONLY_VISIBLE
            )
    };

    public static final Item[] BUILD_TOOLS = new Item[]{
            Item.of(
                    player -> PlayerUtil.smartAddItemStack(
                            player,
                            ItemStack.of(Material.DEBUG_STICK)
                    ),
                    "gui.builder_menu.build_tools.debug_stick",
                    Material.DEBUG_STICK,
                    ALWAYS
            ),
            Item.of(
                    player -> PlayerUtil.smartAddItemStack(
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
                    PARKOUR_ONLY_VISIBLE
            ),
            Item.of(
                    SpawnPointItem.INSTANCE,
                    "gui.builder_menu.custom_items.spawn_point",
                    "hud/hotbar/spawn_point",
                    ALWAYS
            )
    };

    private static ItemCondition variant(MapVariant variant) {
        return (world, player) -> TriState.byBoolean(world.map().settings().getVariant() == variant);
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
            if (this.condition.check(MapWorld.forPlayerOptional(player), player) == TriState.TRUE) {
                this.giver.accept(player);
            }
        }

        public boolean isVisible(MapWorld world, Player player) {
            return condition.check(world, player) != TriState.FALSE;
        }
    }

    private static void giveCustomItem(@NotNull Player player, @NotNull ItemHandler item) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        var itemStack = world.itemRegistry().getItemStack(item.id(), null);
        PlayerUtil.smartAddItemStack(player, itemStack);
        player.closeInventory();
    }

    @FunctionalInterface
    private interface ItemCondition {

        TriState check(MapWorld world, Player player);
    }
}
