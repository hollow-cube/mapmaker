package net.hollowcube.mapmaker.map;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.ipc.map.*;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class MapPresentation {
    public static final BadSprite EMPTY_STAR = BadSprite.require("icon/map_tooltip/star_empty");
    private static final BadSprite[] STARS = new BadSprite[] {
        BadSprite.require("icon/map_tooltip/star_0"),
        BadSprite.require("icon/map_tooltip/star_1"),
        BadSprite.require("icon/map_tooltip/star_2"),
        BadSprite.require("icon/map_tooltip/star_3"),
        BadSprite.require("icon/map_tooltip/star_4"),
        BadSprite.require("icon/map_tooltip/star_5"),
    };
    private static final BadSprite[] TOOLTIP_BORDERS = new BadSprite[] {
        BadSprite.require("icon/map_tooltip/quality_0"),
        BadSprite.require("icon/map_tooltip/quality_1"),
        BadSprite.require("icon/map_tooltip/quality_2"),
        BadSprite.require("icon/map_tooltip/quality_3"),
        BadSprite.require("icon/map_tooltip/quality_4"),
        BadSprite.require("icon/map_tooltip/quality_5"),
    };

    public static Component difficultyComponent(MapDifficulty value) {
        var name = value == MapDifficulty.UNRATED
            ? "unknown"
            : value.name().toLowerCase(Locale.ROOT);
        return Component.translatable("gui.play_maps.map_display.difficulty." + name);
    }

    public static BadSprite difficultyIcon(MapDifficulty value) {
        return BadSprite.require(
            "icon/map_tooltip/difficulty_"
                + (value == MapDifficulty.UNRATED || value == MapDifficulty.UNKNOWN
                    ? "unknown"
                    : value.ordinal() - 1)
        );
    }

    public static BadSprite qualityStar(MapQuality value) {
        return STARS[value == MapQuality.UNKNOWN ? 0 : value.ordinal()];
    }

    public static BadSprite qualityBorder(MapQuality value) {
        return TOOLTIP_BORDERS[value == MapQuality.UNKNOWN ? 0 : value.ordinal()];
    }

    public static String sizeIcon(MapSize size) {
        return switch (size) {
            case NORMAL, UNKNOWN -> "house_1";
            case LARGE -> "house_2";
            case MASSIVE -> "house_3";
            case COLOSSAL -> "castle";
            case UNLIMITED -> throw new IllegalArgumentException(
                "unlimited maps have no size icon"
            );
        };
    }

    public static Component sizeComponent(MapSize size) {
        return Component.translatable("map.size." + size.name().toLowerCase(Locale.ROOT));
    }

    public static String dimensionName(MapData map, char classifier) {
        return "mapmaker:map/" + map.id().toString().substring(0, 8) + "/" + classifier;
    }
    // Returns title and lore (mutable list)
    @NonBlocking
    public static @NotNull Map.Entry<Component, List<Component>> createHoverComponents(
        @NotNull MapData map,
        @NotNull Component authorName,
        @Nullable PlayerMapProgress personalProgress,
        int playerProtocolVersion
    ) {
        class Holder {
            static final BadSprite PLAYS_ICON = BadSprite.require("icon/map_tooltip/plays");
            static final Component PLAYS_ICON_TEXT = Component.text(
                PLAYS_ICON.fontChar() + FontUtil.computeOffset(2)
            );
            static final BadSprite LIKES_ICON = BadSprite.require("icon/map_tooltip/likes");
            static final Component LIKES_ICON_TEXT = Component.text(
                LIKES_ICON.fontChar() + FontUtil.computeOffset(2)
            );

            static final int QUALITY_BORDER_WIDTH = 30;
        }

        var title = rewriteWithQualityFont(
            map.quality(),
            MapSettings.getNameSafe(map.settings())
        ).decoration(TextDecoration.ITALIC, false);

        if (!map.listed()) {
            title = title.append(Component.translatable("gui.play_maps.map_display.unlisted"));
        }

        var quality = map.quality() == MapQuality.UNKNOWN ? MapQuality.UNRATED : map.quality();
        var starText = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i <= quality.ordinal()) {
                starText.append(qualityStar(quality).fontChar());
            } else {
                starText.append(EMPTY_STAR.fontChar());
            }
        }

        var isParkour = map.settings().variant() == MapVariant.PARKOUR;
        var lore = new ArrayList<Component>();
        lore.add(Component.translatable("gui.play_maps.map_display.author", authorName));
        lore.add(Component.empty());
        var contentLine1 = Component.empty()
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
            .append(
                Component.text(qualityBorder(quality).fontChar()).shadowColor(ShadowColor.none())
            )
            .append(Component.text(FontUtil.computeOffset(5)));
        if (isParkour)
            contentLine1 = contentLine1.append(difficultyComponent(map.difficulty()))
                .append(Component.text(FontUtil.computeOffset(6)));
        lore.add(contentLine1.append(getMapTypeComponent(map)));
        var difficultyIcon = difficultyIcon(map.difficulty());
        var totalPadding = Holder.QUALITY_BORDER_WIDTH - difficultyIcon.width();
        var leftPadding = (int) Math.ceil(totalPadding / 2.0)
            + (map.difficulty() == MapDifficulty.MEDIUM ? -1 : 0); // Cursed bias for medium :sob:
        var playsLikes = isParkour
            ? Holder.PLAYS_ICON_TEXT
                .append(
                    Component.text(
                        NumberUtil.formatCurrency(map.uniquePlays()),
                        TextColor.color(0xaeaeae)
                    )
                )
                .append(Component.text(FontUtil.computeOffset(6)))
            : Component.empty();
        playsLikes = playsLikes.append(Holder.LIKES_ICON_TEXT)
            .append(Component.text(map.likes(), TextColor.color(0xaeaeae)));
        lore.add(
            Component.empty()
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .append(
                    Component
                        .text(
                            FontUtil.computeOffset(leftPadding)
                                + difficultyIcon.fontChar()
                                + FontUtil.computeOffset(totalPadding - leftPadding)
                        )
                        .shadowColor(ShadowColor.none())
                )
                .append(Component.text(FontUtil.computeOffset(5)))
                .append(Component.text(starText.toString()))
                .append(Component.text(FontUtil.computeOffset(6)))
                .append(playsLikes)
        );
        lore.add(Component.empty());

        var settingsLine = createSettingsLine(map);
        if (settingsLine != null) {
            lore.add(
                Component.empty()
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text(BadSprite.require("icon/map_tooltip/settings").fontChar())
                            .shadowColor(ShadowColor.none())
                    )
                    .append(Component.text(FontUtil.computeOffset(2)))
                    .append(settingsLine)
            );
            lore.add(Component.empty());
        }

        if (playerProtocolVersion < map.protocolVersion()) {
            lore.addAll(
                LanguageProviderV2.translateMulti(
                    "gui.play_maps.map_display.wrongversion",
                    List.of(Component.text(ProtocolVersions.getProtocolName(map.protocolVersion())))
                )
            );
            lore.add(Component.empty());
        } else if (personalProgress != null) {
            var progress = personalProgress.progress();
            var playtime = personalProgress.playtime();
            if (progress == PlayerMapProgress.Progress.COMPLETE) {
                lore.add(
                    Component.empty()
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                            Component
                                .text(BadSprite.require("icon/map_tooltip/completed").fontChar())
                                .shadowColor(ShadowColor.none())
                        )
                        .append(Component.text(FontUtil.computeOffset(6)))
                        .append(
                            Component.translatable(
                                "gui.play_maps.map_display.completed",
                                Component.text(NumberUtil.formatMapPlaytime(playtime, true))
                            )
                        )
                );
                lore.add(Component.empty());
            } else if (progress == PlayerMapProgress.Progress.STARTED && playtime > 0) {
                lore.add(
                    Component.empty()
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                            Component.translatable(
                                "gui.play_maps.map_display.in_progress",
                                Component.text(NumberUtil.formatMapPlaytime(playtime, true))
                            )
                        )
                );
                lore.add(Component.empty());
            }
        }

        return Map.entry(title, lore);
    }

    private static @Nullable Component createSettingsLine(@NotNull MapData map) {
        class Holder {
            static final Component SEPARATOR = Component.text(", ", TextColor.color(0xB0B0B0));
        }

        var components = new ArrayList<ComponentLike>();
        int enabledExtra = 0;
        for (var setting : MapSettings.TOOLTIP_SETTINGS) {
            if (!MapSettings.get(map.settings(), setting)) continue;

            if (!components.isEmpty()) components.add(Holder.SEPARATOR);
            if (components.size() < 4) { // 4 = 2 separators & 2 settings
                components.add(
                    Component.translatable("gui.play_maps.map_display." + setting.key())
                );
            } else enabledExtra++;
        }
        if (enabledExtra > 0) {
            components.add(Holder.SEPARATOR.append(Component.text("+" + enabledExtra)));
        }

        if (components.isEmpty()) return null;
        return Component.textOfChildren(components.toArray(new ComponentLike[0]));
    }

    private static @NotNull Component getMapTypeComponent(@NotNull MapData map) {
        if (map.settings().variant() == MapVariant.PARKOUR) {
            return switch (MapSettings.getParkourSubVariant(map.settings())) {
                case SPEEDRUN -> Component.text("Speedrun Parkour", TextColor.color(0x55ffff));
                case SECTIONED -> Component.text("Sectioned Parkour", TextColor.color(0x55ffff));
                case RANKUP -> Component.text("Rankup Parkour", TextColor.color(0x55ffff));
                case GAUNTLET -> Component.text("Gauntlet Parkour", TextColor.color(0x55ffff));
                case DROPPER -> Component.text("Dropper Parkour", TextColor.color(0x55ffff));
                case ONE_JUMP -> Component.text("One Jump Parkour", TextColor.color(0x55ffff));
                case INFORMATIVE -> Component.text(
                    "Informative Parkour",
                    TextColor.color(0x55ffff)
                );
                case null -> Component.text("Generic Parkour", TextColor.color(0x55ffff));
            };
        } else if (map.settings().variant() == MapVariant.BUILDING) {
            return switch (MapSettings.getBuildingSubVariant(map.settings())) {
                case SHOWCASE -> Component.text("Building Showcase", TextColor.color(0x0B9F0B));
                case TUTORIAL -> Component.text("Building Tutorial", TextColor.color(0x0B9F0B));
                case null -> Component.text("Generic Building", TextColor.color(0x0B9F0B));
            };
        } else {
            return Component.text("Adventure Map", TextColor.color(0x9F0B0B));
        }
    }

    public static @NotNull Component rewriteWithQualityFont(
        @NotNull MapQuality quality,
        @NotNull String text
    ) {
        class Holder {
            static final MiniMessage MM = MiniMessage.miniMessage();
        }

        //todo use values directly from placeholders/tx keys rather than duplicating here
        return switch (quality) {
            case UNRATED, UNKNOWN -> Component.text(
                text,
                TextColor.color(0xF2F2F2)
            ); // placeholders.json5 -> white
            case GOOD -> Component.text(
                text,
                TextColor.color(0xF5DC3B)
            ); // placeholders.json5 -> lemon
            case GREAT -> Component.text(
                text,
                TextColor.color(0x8CDB46)
            ); // placeholders.json5 -> toxic_green
            case EXCELLENT -> Holder.MM.deserialize(
                String.format(
                    "<gradient:#81AFFF:#8EB7FF:#9CBFFF:#A9C7FF:#B6CFFF:#A9C7FF:#9CBFFF:#8EB7FF:#81AFFF>%s</gradient>",
                    text
                )
            ); // gui.map_details.map_info_tab.quality.excellent.name
            case OUTSTANDING -> Holder.MM.deserialize(
                String.format(
                    "<gradient:#78FFDF:#88FFE4:#98FFE9:#A8FFEE:#B8FFF3:#C8FFF8:#B8FFF3:#A8FFEE:#98FFE9:#88FFE4:#78FFDF>%s</gradient>",
                    text
                )
            ); // gui.map_details.map_info_tab.quality.outstanding.name;
            case MASTERPIECE -> Holder.MM.deserialize(
                String.format(
                    "<gradient:#EE6EFF:#F17CFE:#F58BFE:#F899FD:#FCA8FD:#FFB6FC:#FCA8FD:#F899FD:#F58BFE:#F17CFE:#EE6EFF>%s</gradient>",
                    text
                )
            ); // gui.map_details.map_info_tab.quality.masterpiece.name;
        };
    }
}
