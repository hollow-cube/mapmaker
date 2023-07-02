package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.hub.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public class EditMap extends View {
    private static final System.Logger logger = System.getLogger(EditMap.class.getSimpleName());

    private @ContextObject("handler") HubHandler mapHandler;
    private @ContextObject MapService mapService;

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("publish") Label publishButton;

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
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to edit map")); //todo use translation key
            MinecraftServer.getExceptionManager().handleException(e);
        } finally {
            player.closeInventory();
        }
    }

    @Action(value = "publish", async = true)
    private @Blocking void publishMap(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);
        MapData publishedMap;
        try {
            publishButton.setState(State.LOADING);
            publishedMap = mapService.publishMap(playerData.id(), map.id());
        } catch (Exception e) {
            //todo record this exception in sentry or something
            logger.log(System.Logger.Level.ERROR, "Failed to publish map", e);
            return;
        } finally {
            publishButton.setState(State.ACTIVE);
        }

        EventDispatcher.call(new MapDeletedEvent(map.id())); //todo this event is still scuffed
        performSignal(CreateMaps.SIG_RESET);
        pushView(c -> new MapDetailsView(c, publishedMap, Component.text(publishedMap.owner())));
    }

    // MAP NAME EDITING

    @Action("map_name")
    private @NonBlocking void beginUpdateMapName() {
        pushView(c -> {
            var view = new SetMapName(c);
            view.showMap(map.settings().getName());
            return view;
        });
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

    // MAP ICON EDITING

    @Action("map_icon")
    private @NonBlocking void beginUpdateMapIcon() {
        pushView(SetMapIcon::new);
    }

    @Signal(SetMapIcon.SIG_UPDATE_ICON)
    private @NonBlocking void finishUpdateMapIcon(@NotNull Material newMaterial) {
        map.settings().setIcon(newMaterial);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(),
                    new MapUpdateRequest().setIcon(newMaterial.name()));
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    /** Sets the elements to have the latest info from the map. */
    private void updateElementsFromMap() {
        mapNameText.setText(map.settings().getName()); //todo handle missing
        //todo update map icon lore to include material
    }

    // TAB SWITCHING

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
