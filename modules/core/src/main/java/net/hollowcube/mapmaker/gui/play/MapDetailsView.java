package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MapDetailsView extends View {
    private static final Logger logger = LoggerFactory.getLogger(MapDetailsView.class);

    private @ContextObject ServerBridge bridge;

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_info_switch") Switch tabInfoSwitch;
    private @Outlet("tab_stats_switch") Switch tabStatsSwitch;
    private @Outlet("tab_times_switch") Switch tabTimesSwitch;
    private @Outlet("tab_reviews_switch") Switch tabReviewswitch;
    private Switch[] tabSwitches;

    // MAP QUALITIES (leave what is commented out, refer to line 92)
//    private @Outlet("quality_switch") Switch qualitySwitch;
    private @Outlet("quality_unrated_half_text") Text qualityUnratedHalfText;
    private @Outlet("quality_unrated_full_text") Text qualityUnratedFullText;
//    private @Outlet("quality_good_text") Text qualityGoodText;
//    private @Outlet("quality_great_text") Text qualityGreatText;
//    private @Outlet("quality_excellent_text") Text qualityExcellentText;
//    private @Outlet("quality_outstanding_text") Text qualityOutstandingText;
//    private @Outlet("quality_masterpiece_text") Text qualityMasterpieceText;

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
    private @Outlet("map_settings_text") Text mapSettingsText;

    // GENERAL
    private @Outlet("variant_icon_switch") Switch variantIconSwitch;
    private @Outlet("title") Text titleText;
    private @Outlet("author") Text authorText;

    private final PersonalizedMapData map;

    public MapDetailsView(@NotNull Context context, @NotNull PersonalizedMapData map, @NotNull Component authorName) {
        super(context);
        this.map = map;

        this.tabSwitches = new Switch[]{tabInfoSwitch, tabStatsSwitch, tabTimesSwitch, tabReviewswitch};
        selectTab(0);

        variantIconSwitch.setOption(map.settings().getVariant().ordinal());

        // MAP QUALITY

//        INTENTIONALLY DISABLED, DO NOT UNCOMMENT UNTIL METHODS ARE IMPLEMENTED. THIS ALSO NEEDS A SMALL REWORK.
//        Switch[] qualitySwitches = new Switch[]{qualitySwitch};
//        if(map.getQuality().equals("good")) {
//            qualityGoodText.setText("Good", TextColor.color(0xF5DC3B));
//            qualitySwitch.setOption(1);
//        } else if(map.getQualityName().equals("great")) {
//            qualityGreatText.setText("Great", TextColor.color(0x8CDB46));
//            qualitySwitch.setOption(2);
//        } else if(map.getQualityName().equals("excellent")) {
//            qualityExcellentText.setText("Excellent", TextColor.color(0x81AFFF));
//            qualitySwitch.setOption(3);
//        } else if(map.getQualityName().equals("outstanding")) {
//            qualityOutstandingText.setText("Outstanding", TextColor.color(0x78FFDF));
//            qualitySwitch.setOption(4);
//        } else if(map.getQualityName().equals("masterpiece")) {
//            qualityMasterpieceText.setText("Masterpiece", TextColor.color(0xEE6EFF));
//            qualitySwitch.setOption(5);
//        } else {
//        Default value for map quality (unrated)
//        qualityUnratedText.setText("Unrated", TextColor.color(0xF04B3D));
//        qualitySwitch.setOption(0);
//        }

        // MAP DIFFICULTY

        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            rowOneSwitch.setOption(1);
            qualityUnratedHalfText.setText("Unrated", TextColor.color(0xF04B3D));
            if (map.getUniquePlays() > 1) { //todo make >1 later
                if (map.getDifficultyName().equals("easy")) {
                    difficultyEasyText.setText("Easy", TextColor.color(0x46FA32));
                    difficultySwitch.setOption(1);
                } else if (map.getDifficultyName().equals("medium")) {
                    difficultyMediumText.setText("Medium", TextColor.color(0xFFE11C));
                    difficultySwitch.setOption(2);
                } else if (map.getDifficultyName().equals("hard")) {
                    difficultyHardText.setText("Hard", TextColor.color(0xFA8C34));
                    difficultySwitch.setOption(3);
                } else if (map.getDifficultyName().equals("expert")) {
                    difficultyExpertText.setText("Expert", TextColor.color(0xE6464F));
                    difficultySwitch.setOption(4);
                } else if (map.getDifficultyName().equals("nightmare")) {
                    difficultyNightmareText.setText("Nightmare", TextColor.color(0xCC216D));
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
                mapTypeSpeedrunText.setText("Speedrun Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(1);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.SECTIONED) {
                mapTypeSectionedText.setText("Sectioned Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(2);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.RANKUP) {
                mapTypeRankupText.setText("Rankup Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(3);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.GAUNTLET) {
                mapTypeGauntletText.setText("Gauntlet Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(4);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.DROPPER) {
                mapTypeDropperText.setText("Dropper Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(5);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.ONE_JUMP) {
                mapTypeOneJumpText.setText("One Jump Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(6);
            } else if (map.settings().getParkourSubVariant() == ParkourSubVariant.INFORMATIVE) {
                mapTypeInformativeText.setText("Informative Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(7);
            } else {
                mapTypeParkourText.setText("Generic Parkour", TextColor.color(0x15ADD3));
                mapTypeSwitch.setOption(0);
            }
        } else if (map.settings().getVariant() == MapVariant.BUILDING) {
            rowOneSwitch.setOption(0);
            difficultySwitch.setOption(0);
            qualityUnratedFullText.setText("Unrated", TextColor.color(0xF04B3D));
            if (map.settings().getBuildingSubVariant() == BuildingSubVariant.SHOWCASE) {
                mapTypeShowcaseText.setText("Building Showcase", TextColor.color(0x0B9F0B));
                mapTypeSwitch.setOption(8);
            } else if (map.settings().getBuildingSubVariant() == BuildingSubVariant.TUTORIAL) {
                mapTypeTutorialText.setText("Building Tutorial", TextColor.color(0x0B9F0B));
                mapTypeSwitch.setOption(9);
            } else {
                mapTypeBuildingText.setText("Generic Building", TextColor.color(0x0B9F0B));
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
            mapSettingsSwitch.setOption(0);
            noMapSettingsText.setText("No Settings");
        } else {
            mapSettingsSwitch.setOption(1);
            mapSettingsText.setText(settings);
            var settingsFullString = map.settings().getSettingsFullString();
            mapSettingsText.setArgs(Component.text(settingsFullString));
        }

        // GENERIC

        titleText.setText(Objects.requireNonNullElse(map.settings().getName(), MapData.DEFAULT_NAME));

        if (authorName instanceof TextComponent tc) {
            //todo this is cursed code.
            if (tc.content().isEmpty()) tc = (TextComponent) tc.children().get(0);
            authorText.setText(tc.content(), Objects.requireNonNullElse(tc.color(), NamedTextColor.WHITE));
        } else {
            var plainAuthorName = PlainTextComponentSerializer.plainText().serialize(authorName);
            authorText.setText(plainAuthorName, TextColor.color(0xB0B0B0));
        }
        authorText.setArgs(authorName);

    }

    @Action(value = "play_map", async = true)
    public void handlePlayMap(@NotNull Player player) {
        try {
            bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.PLAYING);
            player.closeInventory();
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

    private void selectTab(int index) {
        tabSwitch.setOption(index);
        for (int i = 0; i < tabSwitches.length; i++) {
            tabSwitches[i].setOption(i == index ? 1 : 0);
        }
    }
}
