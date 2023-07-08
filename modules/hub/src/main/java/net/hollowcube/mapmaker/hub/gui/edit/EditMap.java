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
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
    private @Outlet("tab_info_switch") Switch tabInfoSwitch;
    private @Outlet("tab_tags_switch") Switch tabTagsSwitch;
    private @Outlet("tab_settings_switch") Switch tabSettingsSwitch;
    private @Outlet("tab_actions_switch") Switch tabActionsSwitch;
    private Switch[] tabSwitches;

    private static final int VERIFY_ERROR = 0;
    private static final int VERIFY = 1;
    private static final int PUBLISH_ERROR = 2;
    private static final int PUBLISH = 3;
    private @Outlet("publish_switch") Switch publishSwitch;
    private @Outlet("publish") Label publishButton;

    // Info tab
    private @Outlet("map_name") Text mapNameText;
    private @Outlet("map_type_tab_switch") Switch mapTypeTabSwitch;

    private MapData map;

    public EditMap(@NotNull Context context) {
        super(context);
        this.tabSwitches = new Switch[]{tabInfoSwitch, tabTagsSwitch, tabSettingsSwitch, tabActionsSwitch};

        selectTab(0);
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
            if (map.verification() != MapVerification.UNVERIFIED) {
                player.sendMessage("there was verification progress but you deleted it by editing the map again, oops");

                var playerData = PlayerDataV2.fromPlayer(player);
                mapService.deleteVerification(playerData.id(), map.id());
            }

            mapHandler.editMap(player, map.id());
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to edit map")); //todo use translation key
            MinecraftServer.getExceptionManager().handleException(e);
        } finally {
            player.closeInventory();
        }
    }

    @Action(value = "verify", async = true)
    private void verifyMap(@NotNull Player player) {
        if (map.verification() == MapVerification.UNVERIFIED) {
            var playerData = PlayerDataV2.fromPlayer(player);
            mapService.beginVerification(playerData.id(), map.id());
        }
        //todo handle errors from begin verify

        // Send the player to the map

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

    private int getPublishState() {
        if (!map.isVerified()) return VERIFY;
        return canPublishMap() ? PUBLISH : PUBLISH_ERROR;
    }

    //todo move this function somewhere where it can be used by the command, etc. maybe some kind of map helpers util class
    private boolean canPublishMap() {
        var settings = map.settings();
        if (settings.getName().isEmpty() || settings.getIcon() == null)
            return false;

        //todo other checks like whether there is a world, parkour tags, etc.

        return true;
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
        final var updateRequest = map.settings().getUpdateRequest();
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(), updateRequest);
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    // MAP ICON EDITING

    private @Outlet("set_map_icon_switch") Switch setMapIconSwitch;
    private @Outlet("set_map_icon_set") Label setMapIconSetLabel;

    @Action("set_map_icon_unset")
    private @NonBlocking void beginUpdateMapIcon1() {
        pushView(SetMapIcon::new);
    }

    @Action("set_map_icon_set")
    private @NonBlocking void beginUpdateMapIcon2() {
        pushView(SetMapIcon::new);
    }

    @Signal(SetMapIcon.SIG_UPDATE_ICON)
    private @NonBlocking void finishUpdateMapIcon(@NotNull Material newMaterial) {
        map.settings().setIcon(newMaterial);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        final var updateRequest = map.settings().getUpdateRequest();
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(), updateRequest);
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    // MAP TYPE SETTINGS

    @Action("map_type_tab_parkour")
    private void selectMapTypeParkourTab() {
        if (mapTypeTabSwitch.getOption() == 0) return;

        mapTypeTabSwitch.setOption(0);
        map.settings().setVariant(MapVariant.PARKOUR);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        final var updateRequest = map.settings().getUpdateRequest();
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(), updateRequest);
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    @Action("map_type_tab_building")
    private void selectMapTypeBuildingTab() {
        if (mapTypeTabSwitch.getOption() == 1) return;

        mapTypeTabSwitch.setOption(1);
        map.settings().setVariant(MapVariant.BUILDING);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        final var updateRequest = map.settings().getUpdateRequest();
        async(() -> {
            mapService.updateMap(player().getUuid().toString(), map.id(), updateRequest);
            //todo if update fails we should revert the name change and indicate to the user that it failed
        });
    }

    /** Sets the elements to have the latest info from the map. */
    private void updateElementsFromMap() {
        // Name
        var name = map.settings().getName();
        if (name.isEmpty()) {
            mapNameText.setText(MapData.DEFAULT_NAME, TextColor.color(0xB0B0B0)); // Light gray color
        } else {
            mapNameText.setText(name);
        }

        // Icon
        var icon = map.settings().getIcon();
        if (icon != null) {
            setMapIconSetLabel.setArgs(Component.text(icon.name()));
            setMapIconSwitch.setOption(1);
        } else {
            setMapIconSwitch.setOption(0);
        }

        // Type
        mapTypeTabSwitch.setOption(map.settings().getVariant().ordinal());

        publishSwitch.setOption(getPublishState());
    }

    // TAB SWITCHING

    @Action("tab_info")
    public void showInfoTab() {
        selectTab(0);
    }

    @Action("tab_stats")
    public void showStatsTab() {
        selectTab(1);
    }

    @Action("tab_settings")
    public void showSettingsTab() {
        selectTab(2);
    }

    @Action("tab_actions")
    public void showActionsTab() {
        selectTab(3);
    }

    private void selectTab(int index) {
        tabSwitch.setOption(index);
        for (int i = 0; i < tabSwitches.length; i++) {
            tabSwitches[i].setOption(i == index ? 1 : 0);
        }
    }

}
