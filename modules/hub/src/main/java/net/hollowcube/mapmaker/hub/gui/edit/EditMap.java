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
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.hub.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.*;
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

    private @ContextObject HubToMapBridge bridge;
    private @ContextObject MapService mapService;

    private @Outlet("slot_id") Text slotIdText;

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_info_switch") Switch tabInfoSwitch;
    private @Outlet("tab_tags_switch") Switch tabTagsSwitch;
    private @Outlet("tab_settings_switch") Switch tabSettingsSwitch;
    private @Outlet("tab_actions_switch") Switch tabActionsSwitch;
    private Switch[] tabSwitches;

    private enum PublishStage {
        VERIFY_ERROR,
        BUILD_ONCE,
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
    private @Outlet("parkour_subvariant_speedrun_switch") Switch parkourSubvariantSpeedrunSwitch;
    private @Outlet("parkour_subvariant_sectioned_switch") Switch parkourSubvariantSectionedSwitch;
    private @Outlet("parkour_subvariant_rankup_switch") Switch parkourSubvariantRankupSwitch;
    private @Outlet("parkour_subvariant_gauntlet_switch") Switch parkourSubvariantGauntletSwitch;
    private @Outlet("parkour_subvariant_dropper_switch") Switch parkourSubvariantDropperSwitch;
    private @Outlet("parkour_subvariant_one_jump_switch") Switch parkourSubvariantOneJumpSwitch;
    private @Outlet("parkour_subvariant_informative_switch") Switch parkourSubvariantInformativeSwitch;
    private final Switch[] parkourSubvariantSwitches;

    // MAP TAGS
    private @Outlet("map_tags_tab_switch") Switch mapTagsTabSwitch;

    // VISUAL
    private @Outlet("map_tag_terrain_switch") Switch mapTagTerrainSwitch;
    private @Outlet("map_tag_organics_switch") Switch mapTagOrganicsSwitch;
    private @Outlet("map_tag_structure_switch") Switch mapTagStructureSwitch;
    private @Outlet("map_tag_interior_switch") Switch mapTagInteriorSwitch;
    private @Outlet("map_tag_music_switch") Switch mapTagMusicSwitch;
    private @Outlet("map_tag_2d_switch") Switch mapTag2DSwitch;
    private @Outlet("map_tag_recreation_switch") Switch mapTagRecreationSwitch;
    private @Outlet("map_tag_story_switch") Switch mapTagStorySwitch;

    //GAMEPLAY
    private @Outlet("map_tag_coop_switch") Switch mapTagCoOpSwitch;
    private @Outlet("map_tag_puzzle_switch") Switch mapTagPuzzleSwitch;
    private @Outlet("map_tag_minigame_switch") Switch mapTagMinigameSwitch;
    private @Outlet("map_tag_exploration_switch") Switch mapTagExplorationSwitch;
    private @Outlet("map_tag_bossbattle_switch") Switch mapTagBossBattleSwitch;
    private @Outlet("map_tag_autocomplete_switch") Switch mapTagAutoCompleteSwitch;
    private @Outlet("map_tag_escape_switch") Switch mapTagEscapeSwitch;
    private @Outlet("map_tag_trivia_switch") Switch mapTagTriviaSwitch;
    private @Outlet("map_tag_strategy_switch") Switch mapTagStrategySwitch;
    private final Switch[] mapTagsSwitches;

    private MapData map;
    private int slot;

    public EditMap(@NotNull Context context) {
        super(context);
        this.tabSwitches = new Switch[]{tabInfoSwitch, tabTagsSwitch, tabSettingsSwitch, tabActionsSwitch};
        this.parkourSubvariantSwitches = new Switch[]{parkourSubvariantSpeedrunSwitch, parkourSubvariantSectionedSwitch,
                parkourSubvariantRankupSwitch, parkourSubvariantGauntletSwitch, parkourSubvariantDropperSwitch,
                parkourSubvariantOneJumpSwitch, parkourSubvariantInformativeSwitch};
        this.mapTagsSwitches = new Switch[]{mapTagCoOpSwitch, mapTagPuzzleSwitch, mapTagEscapeSwitch, mapTagMinigameSwitch,
                mapTagTriviaSwitch, mapTagBossBattleSwitch, mapTagExplorationSwitch, mapTagAutoCompleteSwitch,
                mapTagStrategySwitch, mapTagRecreationSwitch, mapTagTerrainSwitch, mapTagOrganicsSwitch,
                mapTagStructureSwitch, mapTagInteriorSwitch, mapTag2DSwitch, mapTagMusicSwitch, mapTagStorySwitch};
        selectTab(0);
        setState(State.LOADING);
    }

    public void showMap(@NotNull MapData map, int slot) {
        this.map = map;
        this.slot = slot;

        slotIdText.setText(String.format("Slot #%d", slot + 1));

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

            bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.EDITING);
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
            bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.EDITING);
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
        PersonalizedMapData publishedMap2 = new PersonalizedMapData(map, PersonalizedMapData.Progress.NONE);
        pushView(c -> new MapDetailsView(c, publishedMap2, Component.text(publishedMap.owner())));
    }

    private PublishStage getPublishState() {
        try {
            mapService.getLatestSaveState(map.id(), map.owner());
        } catch (MapService.NotFoundError e) {
            return PublishStage.BUILD_ONCE;
//        } catch (Exception e) {
//            logger.log(System.Logger.Level.ERROR, "Player could not access save states when getting map publish state.");
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

    @Action("parkour_subvariant_speedrun_unset")
    private void parkourSubVariantSpeedrunUnset() {
        map.settings().setSubVariant(ParkourSubVariant.SPEEDRUN);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_speedrun_set")
    private void parkourSubVariantSpeedrunSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_sectioned_unset")
    private void parkourSubVariantSectionedUnset() {
        map.settings().setSubVariant(ParkourSubVariant.SECTIONED);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_sectioned_set")
    private void parkourSubVariantSectionedSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_rankup_unset")
    private void parkourSubVariantRankupUnset() {
        map.settings().setSubVariant(ParkourSubVariant.RANKUP);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_rankup_set")
    private void parkourSubVariantRankupSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_gauntlet_unset")
    private void parkourSubVariantGauntletUnset() {
        map.settings().setSubVariant(ParkourSubVariant.GAUNTLET);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_gauntlet_set")
    private void parkourSubVariantGauntletSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_dropper_unset")
    private void parkourSubVariantDropperUnset() {
        map.settings().setSubVariant(ParkourSubVariant.DROPPER);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_dropper_set")
    private void parkourSubVariantDropperSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_one_jump_unset")
    private void parkourSubVariantOneJumpUnset() {
        map.settings().setSubVariant(ParkourSubVariant.ONE_JUMP);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_one_jump_set")
    private void parkourSubVariantOneJumpSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_informative_unset")
    private void parkourSubVariantInformativeUnset() {
        map.settings().setSubVariant(ParkourSubVariant.INFORMATIVE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("parkour_subvariant_informative_set")
    private void parkourSubVariantInformativeSet() {
        map.settings().setSubVariant(null);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_type_tab_building")
    private void selectMapTypeBuildingTab() {
        System.out.println("map_type_tab_building");
        if (mapTypeTabSwitch.getOption() == 1) return;

        mapTypeTabSwitch.setOption(1);
        map.settings().setVariant(MapVariant.BUILDING);
        map.settings().removeGameplayTags();
        updateElementsFromMap();
        updateRequest();
    }

    // MAP TAG TABS

    @Action("map_tags_tab_visual")
    private void selectMapTagVisual() {
        System.out.println("map_tags_tab_visual");
        if (mapTagsTabSwitch.getOption() == 0) return;
        mapTagsTabSwitch.setOption(0);
    }

    @Action("map_tags_tab_gameplay")
    private void selectMapTagGameplay() {
        if (!(map.settings().getVariant() == MapVariant.BUILDING)) {
            System.out.println("map_tags_tab_gameplay");
            if (mapTagsTabSwitch.getOption() == 1) return;
            mapTagsTabSwitch.setOption(1);
        }
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

    // VISUAL TAGS

    @Action("map_tag_terrain_unset")
    private void mapTagTerrainUnset() {
        tagClickHandler(MapTags.Tag.TERRAIN, true);
    }

    @Action("map_tag_terrain_set")
    private void mapTagTerrainSet() {
        tagClickHandler(MapTags.Tag.TERRAIN, false);
    }

    @Action("map_tag_organics_unset")
    private void mapTagOrganicsUnset() {
        tagClickHandler(MapTags.Tag.ORGANICS, true);
    }

    @Action("map_tag_organics_set")
    private void mapTagOrganicsSet() {
        tagClickHandler(MapTags.Tag.ORGANICS, false);
    }

    @Action("map_tag_structure_unset")
    private void mapTagStructureUnset() {
        tagClickHandler(MapTags.Tag.STRUCTURE, true);
    }

    @Action("map_tag_structure_set")
    private void mapTagStructureSet() {
        tagClickHandler(MapTags.Tag.STRUCTURE, false);
    }

    @Action("map_tag_interior_unset")
    private void mapTagInteriorUnset() {
        tagClickHandler(MapTags.Tag.INTERIOR, true);
    }

    @Action("map_tag_interior_set")
    private void mapTagInteriorSet() {
        tagClickHandler(MapTags.Tag.INTERIOR, false);
    }

    @Action("map_tag_music_unset")
    private void mapTagMusicUnset() { // TODO coming later
//        tagClickHandler(MapTags.Tag.MUSIC, true);
    }

    @Action("map_tag_music_set")
    private void mapTagMusicSet() { // TODO coming later
//        tagClickHandler(MapTags.Tag.MUSIC, false);
    }

    @Action("map_tag_2d_unset")
    private void mapTag2DUnset() {
        tagClickHandler(MapTags.Tag.TWODIMENSIONAL, true);
    }

    @Action("map_tag_2d_set")
    private void mapTag2DSet() {
        tagClickHandler(MapTags.Tag.TWODIMENSIONAL, false);
    }

    @Action("map_tag_recreation_unset")
    private void mapTagRecreationUnset() {
        tagClickHandler(MapTags.Tag.RECREATION, true);
    }

    @Action("map_tag_recreation_set")
    private void mapTagRecreationSet() {
        tagClickHandler(MapTags.Tag.RECREATION, false);
    }

    @Action("map_tag_story_unset")
    private void mapTagStoryUnset() {
        tagClickHandler(MapTags.Tag.STORY, true);
    }

    @Action("map_tag_recreation_set")
    private void mapTagStorySet() {
        tagClickHandler(MapTags.Tag.STORY, false);
    }

    // GAMEPLAY TAGS

    @Action("map_tag_coop_unset")
    private void mapTagCoOpUnset() { // TODO coming later
//        tagClickHandler(MapTags.Tag.COOP, true);
    }

    @Action("map_tag_coop_set")
    private void mapTagCoOpSet() { // TODO coming later
//        tagClickHandler(MapTags.Tag.COOP, false);
    }

    @Action("map_tag_puzzle_unset")
    private void mapTagPuzzleUnset() {
        tagClickHandler(MapTags.Tag.PUZZLE, true);
    }

    @Action("map_tag_puzzle_set")
    private void mapTagPuzzleSet() {
        tagClickHandler(MapTags.Tag.PUZZLE, false);
    }

    @Action("map_tag_minigame_unset")
    private void mapTagMinigameUnset() { // TODO coming later
//        tagClickHandler(MapTags.Tag.MINIGAME, true);
    }

    @Action("map_tag_minigame_set")
    private void mapTagMinigameSet() { // TODO coming later
//        tagClickHandler(MapTags.Tag.MINIGAME, false);
    }

    @Action("map_tag_exploration_unset")
    private void mapTagExplorationUnset() {
        tagClickHandler(MapTags.Tag.EXPLORATION, true);
    }

    @Action("map_tag_exploration_set")
    private void mapTagExplorationSet() {
        tagClickHandler(MapTags.Tag.EXPLORATION, false);
    }

    @Action("map_tag_bossbattle_unset")
    private void mapTagBossBattleUnset() {
        tagClickHandler(MapTags.Tag.BOSSBATTLE, true);
    }

    @Action("map_tag_bossbattle_set")
    private void mapTagBossBattleSet() {
        tagClickHandler(MapTags.Tag.BOSSBATTLE, false);
    }

    @Action("map_tag_autocomplete_unset") // do this tag at all?
    private void mapTagAutoCompleteUnset() {
        tagClickHandler(MapTags.Tag.AUTOCOMPLETE, true);
    }

    @Action("map_tag_autocomplete_set")
    private void mapTagAutoCompleteSet() {
        tagClickHandler(MapTags.Tag.AUTOCOMPLETE, false);
    }

    @Action("map_tag_escape_unset")
    private void mapTagEscapeUnset() {
        tagClickHandler(MapTags.Tag.ESCAPE, true);
    }

    @Action("map_tag_escape_set")
    private void mapTagEscapeSet() {
        tagClickHandler(MapTags.Tag.ESCAPE, false);
    }

    @Action("map_tag_trivia_unset")
    private void mapTagTriviaUnset() {
        tagClickHandler(MapTags.Tag.TRIVIA, true);
    }

    @Action("map_tag_trivia_set")
    private void mapTagTriviaSet() {
        tagClickHandler(MapTags.Tag.TRIVIA, false);
    }

    @Action("map_tag_strategy_unset")
    private void mapTagStrategyUnset() {
        tagClickHandler(MapTags.Tag.STRATEGY, true);
    }

    @Action("map_tag_strategy_set")
    private void mapTagStrategySet() {
        tagClickHandler(MapTags.Tag.STRATEGY, false);
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

            }
        }

        // TAGS
        var tags = map.settings().getTags();
        for (int i = 0; i < mapTagsSwitches.length; i++) {
            mapTagsSwitches[i].setOption(tags.contains(MapTags.Tag.values()[i]) ? 1 : 0);
        }

        publishSwitch.setOption(getPublishState().ordinal());
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
                    mapPlayerData.unlockedMapSlots(),
                    newMapSlots,
                    mapPlayerData.lastPlayedMap(),
                    mapPlayerData.lastEditedMap()
            ));

            showInfoTab();
            performSignal(CreateMaps.SIG_RESET);
            player.sendMessage("deleted");
        } catch (Exception e) {
            player.sendMessage("failed to delete map");
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
        for (int i = 0; i < tabSwitches.length; i++) {
            tabSwitches[i].setOption(i == index ? 1 : 0);
        }
        // Default to visual tab if map is build type
        if (index == 1 && map != null && map.settings().getVariant().equals(MapVariant.BUILDING))
            mapTagsTabSwitch.setOption(0);
    }

}
