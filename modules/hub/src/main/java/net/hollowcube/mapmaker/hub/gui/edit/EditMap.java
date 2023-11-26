package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class EditMap extends View {
    private static final System.Logger logger = System.getLogger(EditMap.class.getSimpleName());

    private @ContextObject HubToMapBridge bridge;
    private @ContextObject MapService mapService;
    private @ContextObject PlayerService playerService;

    private @OutletGroup("slot_id_.+") Text[] slotIds;
    private @Outlet("tab_switch") Switch tabSwitch;
    private @OutletGroup("tab_.+_switch") Switch[] tabButtonSwitches;

    private enum PublishStage {
        VERIFY_ERROR,
        BUILD_ONCE,
        PLAY_LONGER,
        MISSING_FINISH_PLATE,
        BAD_STATUS_PLATE,
        VERIFY,
        ADD_NAME,
        ADD_ICON,
        PUBLISH;
    }

    private @Outlet("publish_switch") Switch publishSwitch;
    private @Outlet("publish") Label publishButton;

    // INFO TAB
    private @Outlet("map_name") Text mapNameText;
    private @Outlet("map_type_tab_switch") Switch mapTypeTabSwitch;

    // MAP TYPE
    private @OutletGroup("parkour_subvariant_.+_switch") Switch[] parkourSubvariantSwitches;
    private @OutletGroup("building_subvariant_.+_switch") Switch[] buildingSubvariantSwitches;

    // MAP TAGS
    private @Outlet("map_tags_tab_switch") Switch mapTagsTabSwitch;
    // WARNING! The order of the elements in this array MUST match the order of the enum constants in MapTags.Tag.
    // The order is determined by the order in which the elements exist in the GUI xml file.
    private @OutletGroup("map_tag_.+_switch") Switch[] mapTagsSwitches;

    // MAP SETTINGS
    private @Outlet("map_settings_tab_switch") Switch mapSettingsTabSwitch;

    // VISUAL

    // GAMEPLAY
    private @Outlet("map_settings_nojump_switch") Switch mapSettingsNoJump;
    private @Outlet("map_settings_nosprint_switch") Switch mapSettingsNoSprint;
    private @Outlet("map_settings_onlysprint_switch") Switch mapSettingsOnlySprint;
    private @Outlet("map_settings_nosneak_switch") Switch mapSettingsNoSneak;

    private MapData map;
    private int slot;

    public EditMap(@NotNull Context context) {
        super(context);

        setupSubvariantClickHandlers();
        setupTagClickHandlers();
        setupSettingsClickHandlers();

        selectTab(0);
        setState(State.LOADING);
    }

    private void setupSubvariantClickHandlers() {
        // Parkour
        for (var subvariant : ParkourSubVariant.values()) {
            var name = subvariant.name().toLowerCase(Locale.ROOT);

            // Unset handler to set the subvariant on click
            addActionHandler(
                    String.format("parkour_subvariant_%s_unset", name),
                    Label.ActionHandler.lmb(player -> {
                        map.settings().setParkourSubVariant(subvariant);
                        updateElementsFromMap();
                        updateRequest();
                    })
            );

            // Set handler to unset the subvariant on click
            addActionHandler(
                    String.format("parkour_subvariant_%s_set", name),
                    Label.ActionHandler.lmb(player -> {
                        map.settings().setParkourSubVariant(null);
                        updateElementsFromMap();
                        updateRequest();
                    })
            );
        }

        // Building
        for (var subvariant : BuildingSubVariant.values()) {
            var name = subvariant.name().toLowerCase(Locale.ROOT);

            // Unset handler to set the subvariant on click
            addActionHandler(
                    String.format("building_subvariant_%s_unset", name),
                    Label.ActionHandler.lmb(player -> {
                        map.settings().setBuildingSubVariant(subvariant);
                        updateElementsFromMap();
                        updateRequest();
                    })
            );

            // Set handler to unset the subvariant on click
            addActionHandler(
                    String.format("building_subvariant_%s_set", name),
                    Label.ActionHandler.lmb(player -> {
                        map.settings().setBuildingSubVariant(null);
                        updateElementsFromMap();
                        updateRequest();
                    })
            );
        }
    }

    private void setupTagClickHandlers() {
        for (var mapTag : MapTags.Tag.values()) {
            if (mapTag.isDisabled()) continue;
            var name = mapTag.name().toLowerCase(Locale.ROOT);

            // Unset handler to set the tag on click
            addActionHandler(
                    String.format("map_tag_%s_unset", name),
                    Label.ActionHandler.lmb(player -> tagClickHandler(mapTag, true))
            );

            // Set handler to unset the tag on click
            addActionHandler(
                    String.format("map_tag_%s_set", name),
                    Label.ActionHandler.lmb(player -> tagClickHandler(mapTag, false))
            );
        }
    }

    private void setupSettingsClickHandlers() {
        for (var mapSetting : MapSettings.Setting.values()) {
            var name = mapSetting.name().toLowerCase(Locale.ROOT);

            // Unset handler to set the setting on click
            addActionHandler(
                    String.format("map_settings_%s_unset", name),
                    Label.ActionHandler.lmb(player -> settingClickHandler(mapSetting, true))
            );

            // Set handler to unset the setting on click
            addActionHandler(
                    String.format("map_settings_%s_set", name),
                    Label.ActionHandler.lmb(player -> settingClickHandler(mapSetting, false))
            );
        }
    }

    public void showMap(@NotNull MapData map, int slot) {
        this.map = map;
        this.slot = slot;

        for (var slotId : slotIds) {
            slotId.setText(String.format("Slot #%d", slot + 1));
            slotId.setArgs(Component.text(slot + 1));
        }

        updateElementsFromMap();
        setState(State.ACTIVE);
    }

    @Action(value = "edit_in_world", async = true)
    private @Blocking void editMap(@NotNull Player player) {
        try {
            if (map.verification() != MapVerification.UNVERIFIED) {
                player.sendMessage(Component.translatable("progress.verification.lost"));

                var playerData = PlayerDataV2.fromPlayer(player);
                mapService.deleteVerification(playerData.id(), map.id());
            }

            bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.EDITING);
        } catch (Exception e) {
            player.sendMessage(Component.translatable("edit.map.failure"));
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
            bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.PLAYING);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to verify map")); //todo use translation key
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

        // In case back is used, we need to reset the map details view
        performSignal(CreateMaps.SIG_RESET);

        // Open the map details view for the newly published map
        var authorName = playerService.getPlayerDisplayName2(publishedMap.owner());
        PersonalizedMapData publishedMap2 = new PersonalizedMapData(map, PersonalizedMapData.Progress.NONE);
        pushView(c -> new MapDetailsView(c, publishedMap2, authorName.build(DisplayName.Context.PLAIN)));
    }

    private static final int MIN_PLAYTIME = 5 * 60 * 1000; // 5 minutes

    private PublishStage getPublishState() {
        try {
            var ss = mapService.getLatestSaveState(map.id(), map.owner(), SaveStateType.EDITING);
            var min_playtime = System.getenv("MIN_PLAYTIME") == null ? MIN_PLAYTIME : 0;
            if (ss.getPlaytime() < min_playtime)
                return PublishStage.PLAY_LONGER;
        } catch (MapService.NotFoundError e) {
            return PublishStage.BUILD_ONCE;
        }
        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            boolean found = false;
            for (var object : map.objects()) {
                if (object.type().id().equals("mapmaker:finish_plate")) {
                    found = true;
                    break;
                }
            }
            if (!found) return PublishStage.MISSING_FINISH_PLATE;
        } else {
            for (var object : map.objects()) {
                var objectId = object.type().id();
                if (objectId.equals("mapmaker:checkpoint_plate") || objectId.equals("mapmaker:finish_plate")) {
                    //todo this should be done using the required_variant property on the ObjectType in the future, not sure how to handle messaging then.
                    return PublishStage.BAD_STATUS_PLATE;
                }
            }
        }
        if (!map.isVerified()) return PublishStage.VERIFY;
        if (map.settings().getName().isEmpty()) return PublishStage.ADD_NAME;
        if (map.settings().getIcon() == null) return PublishStage.ADD_ICON;
        return PublishStage.PUBLISH;
    }

    // MAP NAME EDITING

    @Action("map_name")
    private @NonBlocking void beginUpdateMapName() {
        pushView(c -> new SetMapName(c, map.settings().getName()));
    }

    @Signal(SetMapName.SIG_UPDATE_NAME)
    private @NonBlocking void finishUpdateMapName(@NotNull String newName) {
        int maxLength = 20;
        //TODO make this only update the display of the name in the GUI, appending ... to the end, and not messing with the actual name
        String limitedName = newName.length() > maxLength ? newName.substring(0, maxLength) : newName;

        map.settings().setName(limitedName);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        async(() -> map.settings().withUpdateRequest(req -> {
            //todo if update fails we should revert the name change and indicate to the user that it failed
            try {
                mapService.updateMap(player().getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to update map name", e);
                MinecraftServer.getExceptionManager().handleException(e);
                return false;
            }
        }));
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

        async(() -> map.settings().withUpdateRequest(req -> {
            //todo if update fails we should revert the name change and indicate to the user that it failed
            try {
                mapService.updateMap(player().getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to update map icon", e);
                MinecraftServer.getExceptionManager().handleException(e);
                return false;
            }
        }));
    }

    private void updateRequest() {
        async(() -> map.settings().withUpdateRequest(req -> {
            //todo if update fails we should revert the name change and indicate to the user that it failed
            try {
                mapService.updateMap(player().getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to update map", e);
                MinecraftServer.getExceptionManager().handleException(e);
                return false;
            }
        }));
    }

    // MAP TYPE SETTINGS

    @Action("map_type_tab_parkour")
    private void selectMapTypeParkourTab() {
        if (mapTypeTabSwitch.getOption() == 0) return;

        mapTypeTabSwitch.setOption(0);
        map.settings().setVariant(MapVariant.PARKOUR);
        map.settings().removeVisualTags();
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_type_tab_building")
    private void selectMapTypeBuildingTab() {
        if (mapTypeTabSwitch.getOption() == 1) return;

        mapTypeTabSwitch.setOption(1);
        map.settings().setVariant(MapVariant.BUILDING);
        map.settings().removeGameplayTags();
        map.settings().setOnlySprint(false);
        map.settings().setNoSprint(false);
        map.settings().setNoJump(false);
        map.settings().setNoSneak(false);
        updateElementsFromMap();
        updateRequest();
    }

    // MAP TAG TABS

    @Action("map_tags_tab_visual")
    private void selectMapTagVisual() {
        if (mapTagsTabSwitch.getOption() == 0) return;
        mapTagsTabSwitch.setOption(0);
    }

    @Action("map_tags_tab_gameplay")
    private void selectMapTagGameplay() {
        if (mapTagsTabSwitch.getOption() == 1 || map.settings().getVariant() == MapVariant.BUILDING) return;
        mapTagsTabSwitch.setOption(1);
    }

    // TAG HELPER FUNCTIONS

    private boolean canAddAnotherTag(MapTags.TagType tagType) {
        var visualTags = map.settings().getTags().stream().filter(
                tag -> tag.getType() == MapTags.TagType.VISUAL
        ).toList();
        if (map.settings().getVariant() == MapVariant.BUILDING) {
            // Max 3 visual tags
            return visualTags.size() < 3;
        } else if (map.settings().getVariant() == MapVariant.PARKOUR) {
            // Max 2 visual and 2 gameplay tags
            var gameplayTags = map.settings().getTags().stream().filter(
                    tag -> tag.getType() == MapTags.TagType.GAMEPLAY
            ).toList();
            if (tagType.equals(MapTags.TagType.VISUAL) && visualTags.size() >= 2)
                return false;
            return !tagType.equals(MapTags.TagType.GAMEPLAY) || gameplayTags.size() < 2;
        } else {
            System.out.println("unsupported map variant type");
        }
        return true;
    }

    private void tagClickHandler(MapTags.Tag tag, boolean set) {
        if (set) {
            if (canAddAnotherTag(tag.getType())) {
                map.settings().addTag(tag);
            }
        } else {
            map.settings().removeTag(tag);
        }
        updateElementsFromMap();
        updateRequest();
    }

    // MAP SETTING TABS

    @Action("map_settings_tab_visual")
    private void selectMapSettingVisual() {
        if (mapSettingsTabSwitch.getOption() == 0) return;
        mapSettingsTabSwitch.setOption(0);
    }

    @Action("map_settings_tab_gameplay")
    private void selectMapSettingGameplay() {
        if (mapSettingsTabSwitch.getOption() == 1 || map.settings().getVariant() == MapVariant.BUILDING) return;
        mapSettingsTabSwitch.setOption(1);
    }

    private void settingClickHandler(MapSettings.Setting setting, boolean set) {
        if(map.isVerified()) {
            player().sendMessage(Component.translatable("settings.verify.error"));
            return;
        }
        // TODO this is disgusting but I'm lazy, we should do this like tags as enum
        if (set) {
            if (setting.equals(MapSettings.Setting.NOSPRINT)) {
                map.settings().setNoSprint(true);
                map.settings().setOnlySprint(false);
            } else if (setting.equals(MapSettings.Setting.ONLYSPRINT)) {
                map.settings().setOnlySprint(true);
                map.settings().setNoSprint(false);
            } else if (setting.equals(MapSettings.Setting.NOJUMP)) {
                map.settings().setNoJump(true);
            } else if (setting.equals(MapSettings.Setting.NOSNEAK)) {
                map.settings().setNoSneak(true);
            }
        } else {
            if (setting.equals(MapSettings.Setting.NOSPRINT)) {
                map.settings().setNoSprint(false);
            } else if (setting.equals(MapSettings.Setting.ONLYSPRINT)) {
                map.settings().setOnlySprint(false);
            } else if (setting.equals(MapSettings.Setting.NOJUMP)) {
                map.settings().setNoJump(false);
            } else if (setting.equals(MapSettings.Setting.NOSNEAK)) {
                map.settings().setNoSneak(false);
            }
        }
        updateElementsFromMap();
        updateRequest();
    }

    /**
     * Sets the elements to have the latest info from the map.
     */
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
            var translationKey = String.format(
                    "%s.%s.%s",
                    icon.isBlock() ? "block" : "item",
                    icon.namespace().namespace(),
                    icon.namespace().path()
            );
            setMapIconSetLabel.setArgs(Component.translatable(translationKey));
            setMapIconSwitch.setOption(1);
        } else {
            setMapIconSwitch.setOption(0);
        }

        // Type
        mapTypeTabSwitch.setOption(map.settings().getVariant().ordinal());
        switch (map.settings().getVariant()) {
            case PARKOUR -> {
                var subvariant = map.settings().getParkourSubVariant();
                for (int i = 0; i < parkourSubvariantSwitches.length; i++) {
                    var selected = subvariant != null && subvariant.ordinal() == i;
                    parkourSubvariantSwitches[i].setOption(selected ? 1 : 0);
                }
            }
            case BUILDING -> {
                var subvariant = map.settings().getBuildingSubVariant();
                for (int i = 0; i < buildingSubvariantSwitches.length; i++) {
                    var selected = subvariant != null && subvariant.ordinal() == i;
                    buildingSubvariantSwitches[i].setOption(selected ? 1 : 0);
                }

            }
        }

        // TAGS
        var tags = map.settings().getTags();
        for (int i = 0; i < mapTagsSwitches.length; i++) {
            mapTagsSwitches[i].setOption(tags.contains(MapTags.Tag.values()[i]) ? 1 : 0);
        }

        // Settings
        mapSettingsOnlySprint.setOption(map.settings().isOnlySprint() ? 1 : 0);
        mapSettingsNoSprint.setOption(map.settings().isNoSprint() ? 1 : 0);
        mapSettingsNoJump.setOption(map.settings().isNoJump() ? 1 : 0);
        mapSettingsNoSneak.setOption(map.settings().isNoSneak() ? 1 : 0);

        async(() -> {
            publishSwitch.setOption(getPublishState().ordinal());
        });
    }

    // ACTIONS TAB

    @Action(value = "delete_map", async = true)
    private void deleteMap(@NotNull Player player) {
        try {
            var mapPlayerData = MapPlayerData.fromPlayer(player);
            mapService.deleteMap(mapPlayerData, map.id());

            // Remove the map from the player as a "prediction", we will get
            // the actual update from the service later.
            var newMapSlots = mapPlayerData.mapSlots();
            newMapSlots[slot] = null;
            mapPlayerData.update(new MapPlayerData(
                    mapPlayerData.id(),
                    mapPlayerData.unlockedSlots(),
                    newMapSlots,
                    mapPlayerData.lastPlayedMap(),
                    mapPlayerData.lastEditedMap()
            ));

            showInfoTab();
            performSignal(CreateMaps.SIG_RESET);
            player.sendMessage(Component.translatable("command.map.delete.success"));
        } catch (Exception e) {
            player.sendMessage(Component.translatable("command.map.delete.failure"));
            logger.log(System.Logger.Level.ERROR, "failed to delete map", e);
            player.closeInventory();
        }
    }

    // TAB SWITCHING

    @Action("tab_info")
    public void showInfoTab() {
        selectTab(0);
    }

    @Action("tab_tags")
    public void showTagsTab() {
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
        for (int i = 0; i < tabButtonSwitches.length; i++) {
            tabButtonSwitches[i].setOption(i == index ? 1 : 0);
        }
        // Default to visual tab if map is build type
        if (index == 1 && map != null && map.settings().getVariant().equals(MapVariant.BUILDING))
            mapTagsTabSwitch.setOption(0);
    }

}
