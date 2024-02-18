package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class MapDetailsView extends View {
    private static final Logger logger = LoggerFactory.getLogger(MapDetailsView.class);

    private @ContextObject ServerBridge bridge;
    private @ContextObject PlayerService playerService;
    private @ContextObject SessionManager sessionManager;

    private @OutletGroup("report_button_.+") Label[] reportButtons;

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_info_switch") Switch tabInfoSwitch;
    private @Outlet("tab_stats_switch") Switch tabStatsSwitch;
    private @Outlet("tab_times_switch") Switch tabTimesSwitch;
    private @Outlet("tab_reviews_switch") Switch tabReviewswitch;
    private @Outlet("tab_info_switch_building") Switch tabInfoSwitchBuilding;
    private @Outlet("tab_stats_switch_building") Switch tabStatsSwitchBuilding;
    private @Outlet("tab_reviews_switch_building") Switch tabReviewswitchBuilding;
    private @Outlet("tab_container_switch") Switch tabContainerSwitch;
    private @Outlet("no_map_settings_switch") Switch noMapSettingsSwitch;
    private Switch[] tabSwitches;

    // MAP QUALITIES
    private @Outlet("quality_unrated_half_text") Text qualityUnratedHalfText;
    private @Outlet("quality_unrated_full_text") Text qualityUnratedFullText;
    private @Outlet("quality_good_half_text") Text qualityGoodHalfText;
    private @Outlet("quality_good_full_text") Text qualityGoodFullText;
    private @Outlet("quality_great_half_text") Text qualityGreatHalfText;
    private @Outlet("quality_great_full_text") Text qualityGreatFullText;
    private @Outlet("quality_excellent_half_text") Text qualityExcellentHalfText;
    private @Outlet("quality_excellent_full_text") Text qualityExcellentFullText;
    private @Outlet("quality_outstanding_half_text") Text qualityOutstandingHalfText;
    private @Outlet("quality_outstanding_full_text") Text qualityOutstandingFullText;
    private @Outlet("quality_masterpiece_half_text") Text qualityMasterpieceHalfText;
    private @Outlet("quality_masterpiece_full_text") Text qualityMasterpieceFullText;
    private @Outlet("quality_icon_full") Switch qualityIconFull;
    private @Outlet("quality_icon_half") Switch qualityIconHalf;
    private @Outlet("quality_text_full") Switch qualityTextFull;
    private @Outlet("quality_text_half") Switch qualityTextHalf;

    // MAP DIFFICULTIES
    private @Outlet("difficulty_switch") Switch difficultySwitch;
    private @Outlet("difficulty_none_text") Text difficultyNoneText;
    private @Outlet("difficulty_easy_text") Text difficultyEasyText;
    private @Outlet("difficulty_medium_text") Text difficultyMediumText;
    private @Outlet("difficulty_hard_text") Text difficultyHardText;
    private @Outlet("difficulty_expert_text") Text difficultyExpertText;
    private @Outlet("difficulty_nightmare_text") Text difficultyNightmareText;
    private @Outlet("row_one") Switch rowOneSwitch;

    // MAP TYPES
    private @Outlet("map_type_switch") Switch mapTypeSwitch;

    // PARKOUR
    private @Outlet("map_type_parkour_text") Text mapTypeParkourText;
    private @Outlet("map_type_speedrun_text") Text mapTypeSpeedrunText;
    private @Outlet("map_type_sectioned_text") Text mapTypeSectionedText;
    private @Outlet("map_type_rankup_text") Text mapTypeRankupText;
    private @Outlet("map_type_gauntlet_text") Text mapTypeGauntletText;
    private @Outlet("map_type_dropper_text") Text mapTypeDropperText;
    private @Outlet("map_type_one_jump_text") Text mapTypeOneJumpText;
    private @Outlet("map_type_informative_text") Text mapTypeInformativeText;

    // BUILDING
    private @Outlet("map_type_showcase_text") Text mapTypeShowcaseText;
    private @Outlet("map_type_tutorial_text") Text mapTypeTutorialText;
    private @Outlet("map_type_building_text") Text mapTypeBuildingText;

    // MAP TAGS
    private @Outlet("map_tags_switch") Switch mapTagsSwitch;
    private @Outlet("no_map_tags_text") Text noMapTagsText;
    private @Outlet("map_tags_text") Text mapTagsText;

    // MAP SETTINGS
    private @Outlet("map_settings_switch") Switch mapSettingsSwitch;
    private @Outlet("no_map_settings_text") Text noMapSettingsText;
    private @Outlet("no_map_settings_text_building") Text noMapSettingsTextBuilding;
    private @Outlet("map_settings_text") Text mapSettingsText;

    // GENERAL
    private @Outlet("variant_icon_switch") Switch variantIconSwitch;
    private @Outlet("title") Text titleText;
    private @Outlet("author") Text authorText;

    private final MapData map;

    public MapDetailsView(@NotNull Context context, @NotNull MapData map, @NotNull DisplayName authorName) {
        super(context);
        this.map = map;

        for (var reportButton : reportButtons) {
            var buttonId = Objects.requireNonNull(reportButton.id());
            addActionHandler(buttonId, Label.ActionHandler.lmb(this::handleReportMap));
        }

        if (map.settings().getVariant() == MapVariant.BUILDING) {
            tabContainerSwitch.setOption(0);
            this.tabSwitches = new Switch[]{tabInfoSwitchBuilding, tabStatsSwitchBuilding, tabReviewswitchBuilding};
        } else {
            tabContainerSwitch.setOption(1);
            this.tabSwitches = new Switch[]{tabInfoSwitch, tabStatsSwitch, tabTimesSwitch, tabReviewswitch};
        }

        selectTab(0);

        variantIconSwitch.setOption(map.settings().getVariant().ordinal());

        // MAP QUALITY

        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            if (map.quality() == MapQuality.GOOD) {
                qualityGoodHalfText.setText("Good");
                qualityIconHalf.setOption(1);
                qualityTextHalf.setOption(1);
            } else if (map.quality() == MapQuality.GREAT) {
                qualityGreatHalfText.setText("Great");
                qualityIconHalf.setOption(2);
                qualityTextHalf.setOption(2);
            } else if (map.quality() == MapQuality.EXCELLENT) {
                qualityExcellentHalfText.setText("Excellent");
                qualityIconHalf.setOption(3);
                qualityTextHalf.setOption(3);
            } else if (map.quality() == MapQuality.OUTSTANDING) {
                qualityOutstandingHalfText.setText("Outstanding");
                qualityIconHalf.setOption(4);
                qualityTextHalf.setOption(4);
            } else if (map.quality() == MapQuality.MASTERPIECE) {
                qualityMasterpieceHalfText.setText("Masterpiece");
                qualityIconHalf.setOption(5);
                qualityTextHalf.setOption(5);
            } else {
                qualityUnratedHalfText.setText("Unrated");
                qualityIconHalf.setOption(0);
                qualityTextHalf.setOption(0);
            }
        } else {
            if (map.quality() == MapQuality.GOOD) {
                qualityGoodFullText.setText("Good");
                qualityIconFull.setOption(1);
                qualityTextFull.setOption(1);
            } else if (map.quality() == MapQuality.GREAT) {
                qualityGreatFullText.setText("Great");
                qualityIconFull.setOption(2);
                qualityTextFull.setOption(2);
            } else if (map.quality() == MapQuality.EXCELLENT) {
                qualityExcellentFullText.setText("Excellent");
                qualityIconFull.setOption(3);
                qualityTextFull.setOption(3);
            } else if (map.quality() == MapQuality.OUTSTANDING) {
                qualityOutstandingFullText.setText("Outstanding");
                qualityIconFull.setOption(4);
                qualityTextFull.setOption(4);
            } else if (map.quality() == MapQuality.MASTERPIECE) {
                qualityMasterpieceFullText.setText("Masterpiece");
                qualityIconFull.setOption(5);
                qualityTextFull.setOption(5);
            } else {
                qualityUnratedFullText.setText("Unrated");
                qualityIconFull.setOption(0);
                qualityTextFull.setOption(0);
            }
        }

        // MAP DIFFICULTY

        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            rowOneSwitch.setOption(1);
            if (map.uniquePlays() > PersonalizedMapData.MIN_PLAYS_FOR_DIFFICULTY) {
                if (map.getDifficultyName().equals("easy")) {
                    difficultyEasyText.setText("Easy");
                    difficultySwitch.setOption(1);
                } else if (map.getDifficultyName().equals("medium")) {
                    difficultyMediumText.setText("Medium");
                    difficultySwitch.setOption(2);
                } else if (map.getDifficultyName().equals("hard")) {
                    difficultyHardText.setText("Hard");
                    difficultySwitch.setOption(3);
                } else if (map.getDifficultyName().equals("expert")) {
                    difficultyExpertText.setText("Expert");
                    difficultySwitch.setOption(4);
                } else if (map.getDifficultyName().equals("nightmare")) {
                    difficultyNightmareText.setText("Nightmare");
                    difficultySwitch.setOption(5);
                } else {
                    difficultyNoneText.setText("Unknown");
                    difficultySwitch.setOption(0);
                }
            } else {
                difficultyNoneText.setText("Unknown");
                difficultySwitch.setOption(0);
            }

            // MAP TYPE

            if (map.settings().getParkourSubVariant() == ParkourSubVariant.SPEEDRUN) {
                mapTypeSpeedrunText.setText("Speedrun Parkour");
                mapTypeSwitch.setOption(1);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.SECTIONED) {
                mapTypeSectionedText.setText("Sectioned Parkour");
                mapTypeSwitch.setOption(2);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.RANKUP) {
                mapTypeRankupText.setText("Rankup Parkour");
                mapTypeSwitch.setOption(3);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.GAUNTLET) {
                mapTypeGauntletText.setText("Gauntlet Parkour");
                mapTypeSwitch.setOption(4);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.DROPPER) {
                mapTypeDropperText.setText("Dropper Parkour");
                mapTypeSwitch.setOption(5);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.ONE_JUMP) {
                mapTypeOneJumpText.setText("One Jump Parkour");
                mapTypeSwitch.setOption(6);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.INFORMATIVE) {
                mapTypeInformativeText.setText("Informative Parkour");
                mapTypeSwitch.setOption(7);
            } else {
                mapTypeParkourText.setText("Generic Parkour");
                mapTypeSwitch.setOption(0);
            }
        } else if (map.settings().getVariant() == MapVariant.BUILDING) {
            rowOneSwitch.setOption(0);
            difficultySwitch.setOption(0);
            if (map.settings().getBuildingSubVariant() == BuildingSubVariant.SHOWCASE) {
                mapTypeShowcaseText.setText("Building Showcase");
                mapTypeSwitch.setOption(8);
            } else if (map.settings().getBuildingSubVariant() == BuildingSubVariant.TUTORIAL) {
                mapTypeTutorialText.setText("Building Tutorial");
                mapTypeSwitch.setOption(9);
            } else {
                mapTypeBuildingText.setText("Generic Building");
                mapTypeSwitch.setOption(10);
            }
        }

        // MAG TAGS

        var tagsString = map.settings().getTagsString();
        if (tagsString == null) {
            mapTagsSwitch.setOption(0);
            noMapTagsText.setText("No Tags");
        } else {
            mapTagsSwitch.setOption(1);
            mapTagsText.setText(tagsString);
            var tagsFullString = map.settings().getTagsFullString();
            mapTagsText.setArgs(Component.text(tagsFullString));
        }

        // MAP SETTINGS

        var settings = map.settings().getSettingsString();
        if (settings == null) {
            if (map.settings().getVariant() == MapVariant.BUILDING) {
                noMapSettingsSwitch.setOption(1);
                noMapSettingsTextBuilding.setText("No Settings");
            } else {
                noMapSettingsSwitch.setOption(0);
                noMapSettingsText.setText("No Settings");
            }
            mapSettingsSwitch.setOption(0);
        } else {
            mapSettingsSwitch.setOption(1);
            mapSettingsText.setText(settings);
            var settingsFullString = map.settings().getSettingsFullString();
            mapSettingsText.setArgs(Component.text(settingsFullString));
        }

        // GENERIC

        titleText.setText(Objects.requireNonNullElse(map.settings().getName(), MapData.DEFAULT_NAME));

        String username = authorName.getUsername();
        if (username != null) {
            authorText.setText(username);
        } else {
            // Fall back to uuid
            authorText.setText(map.owner());
        }
        authorText.setArgs(authorName.build(DisplayName.Context.PLAIN));
    }

    public void handleReportMap(@NotNull Player player) {
        if (!CoreFeatureFlags.MAP_REPORTS.test(player)) return;
        pushView(c -> new ReportMapView(c, map.id()));
    }

    @Action(value = "play_map", async = true)
    public void handlePlayMap(@NotNull Player player) {
        // Do not allow players to join maps if they are already in the same map
        var presence = sessionManager.getPresence(PlayerDataV2.fromPlayer(player).id());
        if (presence != null && map.id().equals(presence.mapId())) {
            player.sendMessage(Component.translatable("gui.map_details.already_in_map"));
            player.closeInventory();
            return;
        }

        try {
            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.PLAYING);
        } catch (Exception e) {
            // If an error occurs here the player is still here, it is our responsibility to handle this (with an error)
            logger.error("failed to join map {} for {}: {}", map.id(), PlayerDataV2.fromPlayer(player).id(), e.getMessage());
            player.sendMessage(Component.translatable("command.generic.unknown_error"));
        }
    }

    // TAB SWITCHING

    @Action("tab_info")
    public void showInfoTab() {
        selectTab(0);
    }

    @Action("tab_stats")
    public void showStatsTab() {
//        selectTab(1);
    }

    @Action("tab_times")
    public void showTimesTab() {
        selectTab(2);
    }

    @Action("tab_reviews")
    public void showReviewsTab() {
//        selectTab(3);
    }

    @Action(value = "information", async = true)
    public void showInformation(@NotNull Player player) {
        player.closeInventory();

        var authorName = playerService.getPlayerDisplayName2(map.owner()).build(DisplayName.Context.DEFAULT);
        player.sendMessage(LanguageProviderV2.translateMultiMerged("gui.map_details.map_info_tab.published_id", List.of(
                Component.text(map.id()),
                Component.text(map.publishedIdString()),
                Component.text(map.name()),
                authorName
        )));
    }

    private void selectTab(int index) {
        tabSwitch.setOption(index);
        for (int i = 0; i < tabSwitches.length; i++) {
            tabSwitches[i].setOption(i == index ? 1 : 0);
        }
    }
}
