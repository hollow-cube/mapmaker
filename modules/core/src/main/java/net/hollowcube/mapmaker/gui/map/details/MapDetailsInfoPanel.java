package net.hollowcube.mapmaker.gui.map.details;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapQuality;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

class MapDetailsInfoPanel extends Panel {
    public MapDetailsInfoPanel(@NotNull MapData map) {
        super(9, 4);

        // todo swap for build maps
        var mapQuality = mapQualityInfo(map.quality());
        add(0, 0, new IconRow(4, "gui.map_details.map_info_tab.quality_description",
                "map_details/info/quality/" + mapQuality.key, mapQuality.iconX, mapQuality.iconY,
                "gui.map_details.map_info_tab.quality." + mapQuality.key, mapQuality.name));
        var mapDifficulty = mapDifficultyInfo(map.getDifficulty());
        add(5, 0, new IconRow(3, "gui.map_details.map_info_tab.difficulty_description",
                "map_browser/difficulty/" + mapDifficulty.key, mapDifficulty.iconX, mapDifficulty.iconY,
                "gui.map_details.map_info_tab.difficulty." + mapDifficulty.key, mapDifficulty.name));

        var mapType = mapTypeInfo(map);
        add(0, 1, new IconRow(8, "gui.map_details.map_info_tab.type_description",
                "map_details/info/type/" + mapType.key, mapType.iconX, mapType.iconY,
                "gui.map_details.map_info_tab.type." + mapType.key.replace("pk_", "parkour.")
                        .replace("bd_", "building."), mapType.name));
        var tagsText = map.settings().getTagsString();
        add(0, 2, new IconRow(8, "gui.map_details.map_info_tab.tags_description",
                "map_details/info/tags/icon_" + (tagsText == null ? "off" : "on"), 3, 3,
                tagsText == null ? null : text(map.settings().getTagsFullString()).decoration(TextDecoration.ITALIC, false), tagsText == null ? null : List.of(),
                Objects.requireNonNullElse(tagsText, "No Tags")));
        var settingsText = map.settings().getSettingsString();
        add(0, 3, new IconRow(8, "gui.map_details.map_info_tab.settings_description",
                "map_details/info/settings/icon_" + (settingsText == null ? "off" : "on"), 3, 3,
                settingsText == null ? null : text(map.settings().getSettingsFullString()).decoration(TextDecoration.ITALIC, false), settingsText == null ? null : List.of(),
                Objects.requireNonNullElse(settingsText, "No Settings")));
    }

    private @NotNull RowInfo mapQualityInfo(@NotNull MapQuality quality) {
        return switch (quality) {
            case UNRATED -> new RowInfo("unrated", "Unrated", 3, 3);
            case GOOD -> new RowInfo("good", "Good", 3, 3);
            case GREAT -> new RowInfo("great", "Great", 3, 3);
            case EXCELLENT -> new RowInfo("excellent", "Excellent", 3, 3);
            case OUTSTANDING -> new RowInfo("outstanding", "Outstanding", 3, 3);
            case MASTERPIECE -> new RowInfo("masterpiece", "Masterpiece", 3, 3);
        };
    }

    private @NotNull RowInfo mapDifficultyInfo(@NotNull MapData.Difficulty difficulty) {
        return switch (difficulty) {
            case UNKNOWN -> new RowInfo("unknown", "Unknown", 2, 2);
            case EASY -> new RowInfo("easy", "Easy", 4, 3);
            case MEDIUM -> new RowInfo("medium", "Medium", 3, 3);
            case HARD -> new RowInfo("hard", "Hard", 4, 4);
            case EXPERT -> new RowInfo("expert", "Expert", 4, 3);
            case NIGHTMARE -> new RowInfo("nightmare", "Nightmare", 4, 3);
        };
    }

    private @NotNull RowInfo mapTypeInfo(@NotNull MapData mapData) {
        return switch (mapData.settings().getVariant()) {
            case PARKOUR -> switch (mapData.settings().getParkourSubVariant()) {
                case null -> new RowInfo("pk_generic", "Generic Parkour", 2, 2);
                case SPEEDRUN -> new RowInfo("pk_speedrun", "Speedrun Parkour", 1, 2);
                case SECTIONED -> new RowInfo("pk_sectioned", "Sectioned Parkour", 3, 2);
                case RANKUP -> new RowInfo("pk_rankup", "Rankup Parkour", 3, 2);
                case GAUNTLET -> new RowInfo("pk_gauntlet", "Gauntlet Parkour", 2, 2);
                case DROPPER -> new RowInfo("pk_dropper", "Dropper Parkour", 2, 3);
                case ONE_JUMP -> new RowInfo("pk_one_jump", "One Jump Parkour", 3, 3);
                case INFORMATIVE -> new RowInfo("pk_informative", "Informative Parkour", 2, 3);
            };
            case BUILDING -> switch (mapData.settings().getBuildingSubVariant()) {
                case null -> new RowInfo("bd_generic", "Generic Build", 3, 2);
                case SHOWCASE -> new RowInfo("bd_showcase", "Build Showcase", 4, 3);
                case TUTORIAL -> new RowInfo("bd_tutorial", "Build Tutorial", 3, 3);
            };
            case ADVENTURE -> throw new UnsupportedOperationException("adventure maps");
        };
    }

    private record RowInfo(@NotNull String key, @NotNull String name, int iconX, int iconY) {
    }

    private static class IconRow extends Panel {

        public IconRow(
                int width, @NotNull String iconTranslationKey,
                @NotNull String iconSprite, int iconSpriteX, int iconSpriteY,
                @NotNull String textTranslationKey, @NotNull String text
        ) {
            this(width, iconTranslationKey, iconSprite, iconSpriteX, iconSpriteY,
                    translatable(textTranslationKey + ".name"), LanguageProviderV2.translateMulti(textTranslationKey + ".lore", List.of()),
                    text);
        }

        public IconRow(
                int width, @NotNull String iconTranslationKey,
                @NotNull String iconSprite, int iconSpriteX, int iconSpriteY,
                @NotNull Component textTitle, @NotNull List<Component> textLore, @NotNull String text
        ) {
            super(width + 1, 1);

            add(0, 0, new Button(iconTranslationKey, 1, 1) {{
                disableHoverSprite = true;
            }}
                    .background("generic2/btn/default/1_1")
                    .sprite(iconSprite, iconSpriteX, iconSpriteY));
            add(1, 0, new Text(null, width, 1, text)
                    .align(2, Text.CENTER)
                    .sprite("map_details/info/row_bg_" + width)
                    .text(textTitle, textLore));
        }

    }
}
