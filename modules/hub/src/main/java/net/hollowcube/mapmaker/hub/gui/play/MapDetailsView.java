package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MapDetailsView extends View {
    private static final Logger logger = LoggerFactory.getLogger(MapDetailsView.class);

    private @ContextObject HubHandler handler;

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_info_switch") Switch tabInfoSwitch;
    private @Outlet("tab_stats_switch") Switch tabStatsSwitch;
    private @Outlet("tab_times_switch") Switch tabTimesSwitch;
    private @Outlet("tab_reviews_switch") Switch tabReviewswitch;
    private Switch[] tabSwitches;

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
    private @Outlet("map_type_parkour_text") Text mapTypeParkourText;
    private @Outlet("map_type_speedrun_text") Text mapTypeSpeedrunText;
    private @Outlet("map_type_sectioned_text") Text mapTypeSectionedText;
    private @Outlet("map_type_rankup_text") Text mapTypeRankupText;
    private @Outlet("map_type_gauntlet_text") Text mapTypeGauntletText;
    private @Outlet("map_type_dropper_text") Text mapTypeDropperText;
    private @Outlet("map_type_one_jump_text") Text mapTypeOneJumpText;
    private @Outlet("map_type_informative_text") Text mapTypeInformativeText;

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

        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            Switch[] difficultySwitches = new Switch[]{difficultySwitch};
            rowOneSwitch.setOption(1);
            switch (map.getDifficultyName()) {
                case "easy" -> {
                    difficultyEasyText.setText("Easy", TextColor.color(0x46FA32));
                    difficultySwitch.setOption(1);
                }
                case "medium" -> {
                    difficultyMediumText.setText("Medium", TextColor.color(0xFFE11C));
                    difficultySwitch.setOption(2);
                }
                case "hard" -> {
                    difficultyHardText.setText("Hard", TextColor.color(0xFA8C34));
                    difficultySwitch.setOption(3);
                }
                case "expert" -> {
                    difficultyExpertText.setText("Expert", TextColor.color(0xE6464F));
                    difficultySwitch.setOption(4);
                }
                case "nightmare" -> {
                    difficultyNightmareText.setText("Nightmare", TextColor.color(0xCC216D));
                    difficultySwitch.setOption(5);
                }
                default -> {
                    difficultyNoneText.setText("TBD");
                    difficultySwitch.setOption(0);
                }
            }

            Switch[] mapTypeSwitches = new Switch[]{mapTypeSwitch};
            if (!(map.settings().getParkourSubVariant() == null)) {
                switch (Objects.requireNonNull(map.settings().getParkourSubVariant())) {
                    case SPEEDRUN -> {
                        mapTypeSpeedrunText.setText("Speedrun", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(1);
                    }
                    case SECTIONED -> {
                        mapTypeSectionedText.setText("Sectioned", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(2);
                    }
                    case RANKUP -> {
                        mapTypeRankupText.setText("Rankup", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(3);
                    }
                    case GAUNTLET -> {
                        mapTypeGauntletText.setText("Gauntlet", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(4);
                    }
                    case DROPPER -> {
                        mapTypeDropperText.setText("Dropper", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(5);
                    }
                    case ONE_JUMP -> {
                        mapTypeOneJumpText.setText("One Jump", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(6);
                    }
                    case INFORMATIVE -> {
                        mapTypeInformativeText.setText("Informative", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(7);
                    }
                    default -> {
                        mapTypeParkourText.setText("Parkour", TextColor.color(0x15ADD3));
                        mapTypeSwitch.setOption(0);
                    }
                }
            }
        } else if (map.settings().getVariant() == MapVariant.BUILDING) {
            rowOneSwitch.setOption(0);
            difficultySwitch.setOption(0);
        }

        titleText.setText(Objects.requireNonNullElse(map.settings().getName(), MapData.DEFAULT_NAME));

        var plainAuthorName = PlainTextComponentSerializer.plainText().serialize(authorName);
        authorText.setText(plainAuthorName);
    }

    @Action(value = "play_map", async = true)
    public void handlePlayMap(@NotNull Player player) {
        try {
            handler.playMap(player, map.id());
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
        selectTab(1);
    }

    @Action("tab_times")
    public void showTimesTab() {
        selectTab(2);
    }

    @Action("tab_reviews")
    public void showReviewsTab() {
        selectTab(3);
    }

    private void selectTab(int index) {
        tabSwitch.setOption(index);
        for (int i = 0; i < tabSwitches.length; i++) {
            tabSwitches[i].setOption(i == index ? 1 : 0);
        }
    }
}
