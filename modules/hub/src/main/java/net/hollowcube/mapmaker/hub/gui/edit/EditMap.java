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

import java.util.Arrays;

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

    private static final int VERIFY_ERROR = 0;
    private static final int VERIFY = 1;
    private static final int PUBLISH_ERROR = 2;
    private static final int PUBLISH = 3;
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
    private @Outlet("map_tag_boss_battle_switch") Switch mapTagBossBattleSwitch;
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
        updateElementsFromMap();

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

    // MAP TAGS

    @Action("map_tags_tab_visual")
    private void selectMapTagVisual() {
        System.out.println("map_tags_tab_visual");
        if (mapTagsTabSwitch.getOption() == 0) return;
        mapTagsTabSwitch.setOption(0);
    }

    @Action("map_tags_tab_gameplay")
    private void selectMapTagGameplay() {
        System.out.println("map_tags_tab_gameplay");
        if (mapTagsTabSwitch.getOption() == 1) return;
        mapTagsTabSwitch.setOption(1);
    }

    // VISUAL

    @Action("map_tag_terrain_unset")
    private void mapTagTerrainUnset() {
        System.out.println("map_tag_terrain_unset");
        mapTagTerrainSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.TERRAIN);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_terrain_set")
    private void mapTagTerrainSet() {
        System.out.println("map_tag_terrain_set");
        mapTagTerrainSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.TERRAIN);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_organics_unset")
    private void mapTagOrganicsUnset() {
        System.out.println("map_tag_organics_unset");
        mapTagOrganicsSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.ORGANICS);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_organics_set")
    private void mapTagOrganicsSet() {
        System.out.println("map_tag_organics_set");
        mapTagOrganicsSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.ORGANICS);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_structure_unset")
    private void mapTagStructureUnset() {
        System.out.println("map_tag_structure_unset");
        mapTagStructureSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.STRUCTURE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_structure_set")
    private void mapTagStructureSet() {
        System.out.println("map_tag_structure_set");
        mapTagStructureSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.STRUCTURE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_interior_unset")
    private void mapTagInteriorUnset() {
        System.out.println("map_tag_interior_unset");
        mapTagInteriorSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.INTERIOR);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_interior_set")
    private void mapTagInteriorSet() {
        System.out.println("map_tag_interior_set");
        mapTagInteriorSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.INTERIOR);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_music_unset")
    private void mapTagMusicUnset() {
        System.out.println("map_tag_music_unset");
        mapTagMusicSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.MUSIC);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_music_set")
    private void mapTagMusicSet() {
        System.out.println("map_tag_music_set");
        mapTagMusicSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.MUSIC);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_2d_unset")
    private void mapTag2DUnset() {
        System.out.println("map_tag_2d_unset");
        mapTag2DSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.TWODIMENSIONAL);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_2d_set")
    private void mapTag2DSet() {
        System.out.println("map_tag_2d_set");
        mapTag2DSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.TWODIMENSIONAL);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_recreation_unset")
    private void mapTagRecreationUnset() {
        System.out.println("map_tag_recreation_unset");
        mapTag2DSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.RECREATION);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_recreation_set")
    private void mapTagRecreationSet() {
        System.out.println("map_tag_recreation_set");
        mapTagRecreationSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.RECREATION);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_story_unset")
    private void mapTagStoryUnset() {
        System.out.println("map_tag_story_unset");
        mapTagStorySwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.STORY);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_recreation_set")
    private void mapTagStorySet() {
        System.out.println("map_tag_recreation_set");
        mapTagRecreationSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.STORY);
        updateElementsFromMap();
        updateRequest();
    }

    // GAMEPLAY

    @Action("map_tag_coop_unset")
    private void mapTagCoOpUnset() {
        System.out.println("map_tag_coop_unset");
        mapTagCoOpSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.COOP);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_coop_set")
    private void mapTagCoOpSet() {
        System.out.println("map_tag_coop_set");
        mapTagCoOpSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.COOP);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_puzzle_unset")
    private void mapTagPuzzleUnset() {
        System.out.println("map_tag_puzzle_unset");
        mapTagPuzzleSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.PUZZLE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_puzzle_set")
    private void mapTagPuzzleSet() {
        System.out.println("map_tag_puzzle_set");
        mapTagPuzzleSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.PUZZLE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_minigame_unset")
    private void mapTagMinigameUnset() {
        System.out.println("map_tag_minigame_unset");
        mapTagMinigameSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.MINIGAME);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_minigame_set")
    private void mapTagMinigameSet() {
        System.out.println("map_tag_minigame_set");
        mapTagMinigameSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.MINIGAME);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_exploration_unset")
    private void mapTagExplorationUnset() {
        System.out.println("map_tag_exploration_unset");
        mapTagExplorationSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.EXPLORATION);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_exploration_set")
    private void mapTagExplorationSet() {
        System.out.println("map_tag_exploration_set");
        mapTagExplorationSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.EXPLORATION);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_boss_battle_unset")
    private void mapTagBossBattleUnset() {
        System.out.println("map_tag_boss_battle_unset");
        mapTagBossBattleSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.BOSSBATTLE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_boss_battle_set")
    private void mapTagBossBattleSet() {
        System.out.println("map_tag_boss_battle_set");
        mapTagBossBattleSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.BOSSBATTLE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_autocomplete_unset") // do this tag at all?
    private void mapTagAutoCompleteUnset() {
        System.out.println("map_tag_autocomplete_unset");
        mapTagAutoCompleteSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.AUTOCOMPLETE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_autocomplete_set")
    private void mapTagAutoCompleteSet() {
        System.out.println("map_tag_autocomplete_set");
        mapTagAutoCompleteSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.AUTOCOMPLETE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_escape_unset")
    private void mapTagEscapeUnset() {
        System.out.println("map_tag_escape_unset");
        mapTagEscapeSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.ESCAPE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_escape_set")
    private void mapTagEscapeSet() {
        System.out.println("map_tag_escape_set");
        mapTagEscapeSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.ESCAPE);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_trivia_unset")
    private void mapTagTriviaUnset() {
        System.out.println("map_tag_trivia_unset");
        mapTagTriviaSwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.TRIVIA);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_trivia_set")
    private void mapTagTriviaSet() {
        System.out.println("map_tag_trivia_set");
        mapTagTriviaSwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.TRIVIA);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_strategy_unset")
    private void mapTagStrategyUnset() {
        System.out.println("map_tag_strategy_unset");
        mapTagStrategySwitch.setOption(1);
        map.settings().addTag(MapTags.Tag.TRIVIA);
        updateElementsFromMap();
        updateRequest();
    }

    @Action("map_tag_strategy_set")
    private void mapTagStrategySet() {
        System.out.println("map_tag_strategy_set");
        mapTagStrategySwitch.setOption(0);
        map.settings().removeTag(MapTags.Tag.AUTOCOMPLETE);
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

            }
        }

        // Tags
        var tags = map.settings().getTags();
        var tagEnums = MapTags.Tag.values();
        for (int i = 0; i < mapTagsSwitches.length; i++) {
            mapTagsSwitches[i].setOption(tags.contains(MapTags.Tag.values()[i]) ? 1 : 0);
        }
        System.out.println("mapTagsSwitches: " + Arrays.toString(mapTagsSwitches));
        System.out.println("got map tags " + tags.toString());

        publishSwitch.setOption(getPublishState());
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
