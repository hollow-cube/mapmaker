package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.edit.item.EnterTestModeItem;
import net.hollowcube.mapmaker.map.feature.edit.item.SpawnPointItem;
import net.hollowcube.mapmaker.map.feature.play.item.MapDetailsItem;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CustomItemsView extends View {

    private @ContextObject("bridge") ServerBridge bridge;

    public CustomItemsView(@NotNull Context context) {
        super(context);
    }

    @Action("give_details_item")
    private void giveDetailsItem(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, MapDetailsItem.INSTANCE);
    }

    @Action("give_test_mode_item")
    private void giveTestModeItem(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, EnterTestModeItem.INSTANCE);
    }

    @Action("give_spawn_point_item")
    private void giveSpawnPointItem(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, SpawnPointItem.INSTANCE);
    }

    @Action("exit_map")
    private void giveExitMapItem(@NotNull Player player) {
        player.closeInventory();
        bridge.joinHub(player);
    }
}
