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

    public CustomItemsView(@NotNull Context context) {
        super(context);
    }

    @Action("give_details_item")
    private void giveDetailsItem(@NotNull Player player) {
        //BuilderMenuView.giveCustomItem(player, MapDetailsItem.INSTANCE);
        //todo give an editing version of the map details item that lets them edit the name, display item, tags, etc.
        //todo of their map. The bottom 2 3x3 buttons would be the closed blue doors, since you are already in your map
    }

    @Action("give_test_mode_item")
    private void giveTestModeItem(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, EnterTestModeItem.INSTANCE);
    }

    @Action("give_spawn_point_item")
    private void giveSpawnPointItem(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, SpawnPointItem.INSTANCE);
    }
}
