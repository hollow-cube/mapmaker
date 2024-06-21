package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.gui.common.ConfirmAction;
import net.hollowcube.mapmaker.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
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

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EditMap extends View {
    private static final System.Logger logger = System.getLogger(EditMap.class.getSimpleName());

    private @ContextObject ServerBridge bridge;
    private @ContextObject MapService mapService;
    private @ContextObject PlayerService playerService;

    private @OutletGroup("slot_id_.+") Text[] slotIds;
    private @Outlet("tab_switch") Switch tabSwitch;
    private @OutletGroup("tab_.+_switch") Switch[] tabButtonSwitches;

    private @OutletGroup("info_button_.+") Label[] infoButtons;

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
    private @Outlet("map_tags_locked_container_switch") Switch mapTagsLockedContainerSwitch;
    private @Outlet("map_tags_max_counter") Text mapTagsMaxCounterText;

    // MAP SETTINGS
    private @Outlet("map_settings_tab_switch") Switch mapSettingsTabSwitch;
    private @Outlet("map_settings_locked_container_switch") Switch mapSettingsLockedContainerSwitch;

    // VISUAL
    private @Outlet("map_settings_time_of_day_switch") Switch mapSettingsTimeOfDay;
    private @Outlet("map_settings_weather_type_switch") Switch mapSettingsWeatherType;

    // GAMEPLAY
    private @Outlet("map_settings_nojump_switch") Switch mapSettingsNoJump;
    private @Outlet("map_settings_nosprint_switch") Switch mapSettingsNoSprint;
    private @Outlet("map_settings_onlysprint_switch") Switch mapSettingsOnlySprint;
    private @Outlet("map_settings_nosneak_switch") Switch mapSettingsNoSneak;
    private @Outlet("map_settings_nospec_switch") Switch mapSettingsNoSpec;

    private MapData map;
    private int slot;

    public EditMap(@NotNull Context context) {
        super(context);

        setupSubvariantClickHandlers();
        setupTagClickHandlers();
        setupSettingsClickHandlers();

        for (var button : infoButtons) {
            var buttonId = Objects.requireNonNull(button.id());
            addActionHandler(buttonId, Label.ActionHandler.lmb(this::showInformation));
        }

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

            switch (mapSetting.getValueType()) {
                case BOOLEAN -> {
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
                case ENUM -> {
                    var enumClass = Objects.requireNonNull(mapSetting.getValueClass(), "enum settings must have a value class");
                    for (var value : enumClass.getEnumConstants()) {
                        addActionHandler(
                                String.format("map_settings_%s_%s", name, value.name().toLowerCase(Locale.ROOT)),
                                Label.ActionHandler.lmb(player -> settingClickHandler(mapSetting, true))
                        );
                    }
                }
            }
        }
    }

    public void showMap(@NotNull MapData map, int slot) {
        this.map = Objects.requireNonNull(map);
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

            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING, "edit_maps_gui");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("edit.map.failure"));
            MinecraftServer.getExceptionManager().handleException(e);
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
            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.PLAYING, "edit_maps_gui_verify");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("map.verify.fail"));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Action(value = "publish")
    private void publishMap(@NotNull Player player) {
        pushView(context -> new ConfirmAction(context,
                () -> popView("publish_sig", player),
                Component.translatable("Publish your map " + map.name())));
    }

    @Signal("publish_sig")
    private void publishMapLogic(@NotNull Player player) {
        FutureUtil.submitVirtual(() -> {
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
            // We have to "predict" that the map will be removed by the async update sent over Kafka,
            // which has most likely not arrived yet.
            MapPlayerData.fromPlayer(player).mapSlots()[slot] = null;
            performSignal(CreateMaps.SIG_RESET);

            // Open the map details view for the newly published map
            var authorName = playerService.getPlayerDisplayName2(publishedMap.owner());
            pushView(c -> new MapDetailsView(c, map, authorName));
        });
    }

    private static final int MIN_PLAYTIME = ServerRuntime.getRuntime().isDevelopment() ? 1 : 5 * 60 * 1000; // 5 minutes

    private PublishStage getPublishState() {
        try {
            var ss = mapService.getLatestSaveState(map.id(), map.owner(), SaveStateType.EDITING, null);
            var min_playtime = System.getenv("MIN_PLAYTIME") == null ? MIN_PLAYTIME : 0;
            if (ss.getPlaytime() < min_playtime)
                return PublishStage.PLAY_LONGER;
        } catch (MapService.NotFoundError e) {
            return PublishStage.BUILD_ONCE;
        }
        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            boolean found = false;
//            for (var object : map.objects()) {
//                if ("mapmaker:finish_plate".equals(object.type().id())) {
//                    found = true;
//                    break;
//                }
//            }
//            if (!found) return PublishStage.MISSING_FINISH_PLATE;
        } else {
//            for (var object : map.objects()) {
//                var objectId = object.type().id();
//                if (objectId.equals("mapmaker:checkpoint_plate") || objectId.equals("mapmaker:finish_plate")) {
//                    //todo this should be done using the required_variant property on the ObjectType in the future, not sure how to handle messaging then.
//                    return PublishStage.BAD_STATUS_PLATE;
//                }
//            }
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
        map.settings().setBuildingSubVariant(null);
        map.settings().setVariant(MapVariant.PARKOUR);
        map.settings().removeVisualTags();
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_type_tab_building")
    private void selectMapTypeBuildingTab() {
        if (mapTypeTabSwitch.getOption() == 1) return;

        mapTypeTabSwitch.setOption(1);
        map.settings().setParkourSubVariant(null);
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
        fillTagsCounter(MapTags.TagType.VISUAL);
    }

    @Action("map_tags_tab_gameplay")
    private void selectMapTagGameplay() {
        if (mapTagsTabSwitch.getOption() == 1 || map.settings().getVariant() == MapVariant.BUILDING) return;
        mapTagsTabSwitch.setOption(1);
        fillTagsCounter(MapTags.TagType.GAMEPLAY);
    }

    @Action(value = "reset_tags")
    private void resetTags() {
        map.settings().removeGameplayTags();
        map.settings().removeVisualTags();
        updateElementsFromMap();
    }

    private void fillTagsCounter(@NotNull MapTags.TagType tagType) {
        var typedMaxTags = map.settings().getVariant().maxTags(tagType);
        var currentTags = map.settings().getTags().stream()
                .filter(tag -> tag.getType() == tagType)
                .count();
        mapTagsMaxCounterText.setText(String.format("%d/%d Tags", currentTags, typedMaxTags));
    }

    // TAG HELPER FUNCTIONS

    private boolean canAddAnotherTag(MapTags.TagType tagType) {
        var typedMaxTags = map.settings().getVariant().maxTags(tagType);
        return typedMaxTags > map.settings().getTags().stream()
                .filter(tag -> tag.getType() == tagType)
                .count();
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
        switch (map.verification()) {
            case PENDING -> {
                player().sendMessage(Component.translatable("settings.verify.error"));
                return;
            }
            case VERIFIED -> {
                player().sendMessage(Component.translatable("settings.verified.error"));
                return;
            }
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
            } else if (setting.equals(MapSettings.Setting.NOSPEC)) {
                map.setSetting(MapSettings.NO_SPECTATOR, true);
            } else if (setting.equals(MapSettings.Setting.TIME_OF_DAY)) {
                map.setSetting(MapSettings.TIME_OF_DAY, map.getSetting(MapSettings.TIME_OF_DAY).next());
            } else if (setting.equals(MapSettings.Setting.WEATHER_TYPE)) {
                map.setSetting(MapSettings.WEATHER_TYPE, map.getSetting(MapSettings.WEATHER_TYPE).next());
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
            } else if (setting.equals(MapSettings.Setting.NOSPEC)) {
                map.setSetting(MapSettings.NO_SPECTATOR, false);
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
        fillTagsCounter(mapTagsTabSwitch.getOption() == 0 ? MapTags.TagType.VISUAL : MapTags.TagType.GAMEPLAY);
        var tags = map.settings().getTags();
        for (int i = 0; i < mapTagsSwitches.length; i++) {
            mapTagsSwitches[i].setOption(tags.contains(MapTags.Tag.values()[i]) ? 1 : 0);
        }

        // Settings
        mapSettingsOnlySprint.setOption(map.settings().isOnlySprint() ? 1 : 0);
        mapSettingsNoSprint.setOption(map.settings().isNoSprint() ? 1 : 0);
        mapSettingsNoJump.setOption(map.settings().isNoJump() ? 1 : 0);
        mapSettingsNoSneak.setOption(map.settings().isNoSneak() ? 1 : 0);
        mapSettingsNoSpec.setOption(map.getSetting(MapSettings.NO_SPECTATOR) ? 1 : 0);
        mapSettingsTimeOfDay.setOption(map.getSetting(MapSettings.TIME_OF_DAY).ordinal());
        mapSettingsWeatherType.setOption(map.getSetting(MapSettings.WEATHER_TYPE).ordinal());

        async(() -> {
            publishSwitch.setOption(getPublishState().ordinal());
        });
    }

    // ACTIONS TAB

    @Action(value = "delete_map", async = true)
    private void deleteMap(@NotNull Player player) {
        pushView(context -> new ConfirmAction(context, () -> deleteMapLogic(player),
                Component.translatable("Delete your map " + map.name())));
    }

    private void deleteMapLogic(@NotNull Player player) {
        try {
            var mapPlayerData = MapPlayerData.fromPlayer(player);
            mapService.deleteMap(mapPlayerData.id(), map.id(), null);

            // Remove the map from the player as a "prediction", we will get
            // the actual update from the service later.
            var newMapSlots = mapPlayerData.mapSlots();
            newMapSlots[slot] = null;
            mapPlayerData.update(new MapPlayerData(
                    mapPlayerData.id(),
                    newMapSlots,
                    mapPlayerData.lastPlayedMap(),
                    mapPlayerData.lastEditedMap()
            ));

            performSignal(CreateMaps.SIG_RESET);
            player.sendMessage(Component.translatable("command.map.delete.success"));
            pushView(CreateMaps::new);
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
        if (map.settings().getVariant() == MapVariant.BUILDING) {
            mapTagsLockedContainerSwitch.setOption(0);
        } else {
            mapTagsLockedContainerSwitch.setOption(1);
        }
    }

    @Action("tab_settings")
    public void showSettingsTab() {
        selectTab(2);
        if (map.settings().getVariant() == MapVariant.BUILDING) {
            mapSettingsLockedContainerSwitch.setOption(0);
        } else {
            mapSettingsLockedContainerSwitch.setOption(1);
        }
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
        //Default to visual tab if map is build type
        if (index == 1 && map != null && map.settings().getVariant().equals(MapVariant.BUILDING)) {
            mapTagsTabSwitch.setOption(0);
        }
        if (index == 2 && map != null && map.settings().getVariant().equals(MapVariant.BUILDING)) {
            mapSettingsTabSwitch.setOption(0);
        }
    }

    public void showInformation(@NotNull Player player) {
        player.closeInventory();

        var authorName = playerService.getPlayerDisplayName2(map.owner()).build(DisplayName.Context.DEFAULT);
        player.sendMessage(LanguageProviderV2.translateMultiMerged("gui.map_details.map_info_tab.published_id", List.of(
                Component.text(map.id()),
                Component.text("None/Not Published"),
                Component.text(map.name()),
                authorName
        )));
    }

}
