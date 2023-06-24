package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public class EditMap extends View {
    private static final System.Logger logger = System.getLogger(EditMap.class.getSimpleName());

    private @ContextObject("handler") HubHandler mapHandler;
    private @ContextObject MapService mapService;

    private @Outlet("tab_switch") Switch tabSwitch;

    // Info tab
    private @Outlet("map_name") Text mapNameText;

    private MapData map;

    public EditMap(@NotNull Context context) {
        super(context);

        setState(State.LOADING);
    }

    public void showMap(@NotNull MapData map) {
        this.map = map;

        updateElementsFromMap();
        setState(State.ACTIVE);
    }

    @Action(value = "edit_in_world", async = true)
    private @Blocking void editMap(@NotNull Player player) {
        try {
            mapHandler.editMap(player, map.id());
            player.closeInventory();
        } catch (Exception e) {
            //todo record this exception in sentry or something
            logger.log(System.Logger.Level.ERROR, "Failed to edit map", e);
        }
    }

    @Action(value = "publish", async = true)
    private @Blocking void publishMap(@NotNull Player player) {
//        var playerData = PlayerData.fromPlayer(player);
//        try {
//            mapHandler.publishMap(playerData.getId(), map.getId());
//            player.closeInventory();
//        } catch (Exception e) {
//            //todo record this exception in sentry or something
//            logger.log(System.Logger.Level.ERROR, "Failed to publish map", e);
//        }
    }

    @Action("map_name")
    private @NonBlocking void beginUpdateMapName() {
        pushView(c -> {
            var view = new SetMapName(c);
            view.showMap(map.settings().getName());
            return view;
        });
    }

    @Action("map_icon")
    private @NonBlocking void beginUpdateMapIcon() {
        pushView(SetMapIcon::new);
    }

    @Signal(SetMapName.SIG_UPDATE_NAME)
    private @NonBlocking void finishUpdateMapName(@NotNull String newName) {
        map.settings().setName(newName);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(), new MapUpdateRequest().setName(newName));
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    /** Sets the elements to have the latest info from the map. */
    private void updateElementsFromMap() {
        mapNameText.setText(map.settings().getName()); //todo handle missing
    }

    @Action("tab_info")
    public void showInfoTab() {
        tabSwitch.setOption(0);
    }

    @Action("tab_stats")
    public void showStatsTab() {
        tabSwitch.setOption(1);
    }

    @Action("tab_settings")
    public void showSettingsTab() {
        tabSwitch.setOption(2);
    }

    @Action("tab_actions")
    public void showActionsTab() {
        tabSwitch.setOption(3);
    }

}
