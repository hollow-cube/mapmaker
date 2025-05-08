package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.object.ObjectData;
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
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RuntimeGson
public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    public enum Difficulty {
        UNKNOWN,
        EASY,
        MEDIUM,
        HARD,
        EXPERT,
        NIGHTMARE;

        private static final BadSprite[] TOOLTIP_ICONS = new BadSprite[]{
                BadSprite.require("icon/map_tooltip/difficulty_unknown"),
                BadSprite.require("icon/map_tooltip/difficulty_0"),
                BadSprite.require("icon/map_tooltip/difficulty_1"),
                BadSprite.require("icon/map_tooltip/difficulty_2"),
                BadSprite.require("icon/map_tooltip/difficulty_3"),
                BadSprite.require("icon/map_tooltip/difficulty_4"),
        };

        public @NotNull BadSprite tooltipIcon() {
            return TOOLTIP_ICONS[ordinal()];
        }
    }

    public static final int MIN_PLAYS_FOR_DIFFICULTY = 10;

    public static final String SPAWN_MAP_ID = "b210fa07-d64c-4100-a2db-1426c35b7533";

    private String id;
    private String owner;
    private MapSettings settings;
    private MapVerification verification = MapVerification.UNVERIFIED;

    private long publishedId;
    private Instant publishedAt;

    private int likes;
    private int uniquePlays;
    private double clearRate;

    private MapQuality quality;

    private int objectLimit = 100;
    private List<ObjectData> objects = new ArrayList<>();
    private transient int objectUsage = -1;

    public MapData() {
    }

    public MapData(
            @NotNull String id,
            @NotNull String owner,
            @NotNull MapSettings settings,
            long publishedId,
            @Nullable Instant publishedAt
    ) {
        this.id = id;
        this.owner = owner;
        this.settings = settings;
        this.publishedId = publishedId;
        this.publishedAt = publishedAt;
//        this.pois = new ArrayList<>();
//        this.maxPois = 100;
        this.objectLimit = 100;
        this.objects = new ArrayList<>();
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String name() {
        var name = settings.getName();
        if (name.isEmpty())
            return DEFAULT_NAME;
        return name;
    }

    public @NotNull String owner() {
        return owner;
    }

    public @NotNull MapSettings settings() {
        return settings;
    }

    public <T> @NotNull T getSetting(@NotNull MapSetting<T> setting) {
        return settings.get(setting);
    }

    public <T> void setSetting(@NotNull MapSetting<T> setting, @NotNull T value) {
        settings.set(setting, value);
    }

    public boolean needsVerification() {
        return settings().getVariant() != MapVariant.BUILDING;
    }

    public boolean isVerified() {
        return !needsVerification() || verification == MapVerification.VERIFIED;
    }

    public @NotNull MapVerification verification() {
        return verification;
    }

    public boolean isPublished() {
        return publishedId != 0;
    }

    public long publishedId() {
        return publishedId;
    }

    public @UnknownNullability String publishedIdString() {
        return publishedId == 0 ? null : formatPublishedId(publishedId);
    }

    public @UnknownNullability Instant publishedAt() {
        return publishedAt;
    }

    public int likes() {
        return likes;
    }

    public int uniquePlays() {
        return uniquePlays;
    }

    public double clearRate() {
        return clearRate;
    }

    public @NotNull Difficulty getDifficulty() {
        // Note that this is also computed in the map service currently (though it should just send the
        // difficulty as an enum). If this is changed, make sure to update the service as well.
        if (uniquePlays() < MIN_PLAYS_FOR_DIFFICULTY || settings().getVariant() != MapVariant.PARKOUR)
            return Difficulty.UNKNOWN;
        var cr = clearRate();
        if (cr < 0.05) return Difficulty.NIGHTMARE;
        if (cr < 0.25) return Difficulty.EXPERT;
        if (cr < 0.5) return Difficulty.HARD;
        if (cr < 0.75) return Difficulty.MEDIUM;
        return Difficulty.EASY;
    }

    public @NotNull Component getDifficultyComponent() {
        return Component.translatable("gui.play_maps.map_display.difficulty." + getDifficulty().name().toLowerCase(Locale.ROOT));
    }

    public @NotNull String getDifficultyName() {
        return getDifficulty().name().toLowerCase(Locale.ROOT);
    }

    public @NotNull MapQuality quality() {
        return Objects.requireNonNullElse(quality, MapQuality.UNRATED);
    }

    public int objectLimit() {
        return objectLimit;
    }

    public int objectUsage() {
        if (objectUsage == -1) {
            objectUsage = objects().stream()
                    .mapToInt(o -> o.type().cost())
                    .sum();
        }

        return objectUsage;
    }

    public boolean addObject(@NotNull ObjectData object) {
        settings.updateLock.lock();
        try {
            //todo reenable object limits later
//            if (objectUsage() + object.type().cost() > objectLimit)
//                return false;

            if (objects == null) objects = new ArrayList<>();
            objects.add(object);
            objectUsage += object.type().cost();

            // Add to update
            settings.updates.newObjects.add(object);
            settings.updates.removedObjects.remove(object.id());

            return true;
        } finally {
            settings.updateLock.unlock();
        }
    }

    public boolean removeObject(@NotNull String id) {
        settings.updateLock.lock();
        try {
            if (objects == null) return false;

            var removed = false;
            var iter = objects.iterator();
            while (iter.hasNext()) {
                var object = iter.next();

                if (object.id().equals(id)) {
                    iter.remove();
                    objectUsage = objectUsage() - object.type().cost();
                    removed = true;

                    settings.updates.removedObjects.add(id);
                }
            }

            if (removed) {
                settings.updates.newObjects.removeIf(p -> p.id().equals(id));
            }

            return removed;
        } finally {
            settings.updateLock.unlock();
        }
    }

    public @NotNull List<ObjectData> objects() {
        if (this.objects == null) return List.of();
        return List.copyOf(objects);
    }

    public @Nullable ObjectData getObject(String id) {
        var object = objects().stream().filter(obj -> obj.id().equals(id)).findFirst();
        return object.orElse(null);
    }

    public static @NotNull String formatPublishedId(long number) {
        // Pad zeros if necessary
        var numberString = new StringBuilder(String.valueOf(number));
        while (numberString.length() < 9) {
            numberString.insert(0, "0");
        }

        // Format as xxx-xxx-xxx
        return numberString.substring(0, 3) +
                "-" +
                numberString.substring(3, 6) +
                "-" +
                numberString.substring(6);
    }

    public static long parsePublishedID(@NotNull String publishedId) {
        class Holder {
            static final Pattern ID_PATTERN = Pattern.compile("([0-9]{3})-([0-9]{3})-([0-9]{3})");
        }

        if (!Holder.ID_PATTERN.matcher(publishedId).matches())
            throw new IllegalArgumentException("Invalid published ID format");
        return Long.parseLong(publishedId.replace("-", ""));
    }

    public boolean isCompletable() {
        return settings().getVariant() == MapVariant.PARKOUR;
    }

    public @NotNull String createDimensionName(char classifier) {
        return String.format("mapmaker:map/%s/%s", id().substring(0, 8), classifier);
    }

    public static class WithSlot extends MapData {
        private int slot;

        public int slot() {
            return slot;
        }
    }

    // Returns title and lore (mutable list)
    @NonBlocking
    public static @NotNull Map.Entry<Component, List<Component>> createHoverComponents(
            @NotNull MapData map, @NotNull Component authorName,
            @Nullable Map.Entry<PersonalizedMapData.Progress, Integer> personalProgress
    ) {
        class Holder {
            static final BadSprite PLAYS_ICON = BadSprite.require("icon/map_tooltip/plays");
            static final Component PLAYS_ICON_TEXT = Component.text(PLAYS_ICON.fontChar() + FontUtil.computeOffset(2));
            static final BadSprite LIKES_ICON = BadSprite.require("icon/map_tooltip/likes");
            static final Component LIKES_ICON_TEXT = Component.text(LIKES_ICON.fontChar() + FontUtil.computeOffset(2));

            static final int QUALITY_BORDER_WIDTH = 30;
        }

        var title = MapData.rewriteWithQualityFont(map.quality(), map.settings().getNameSafe())
                .decoration(TextDecoration.ITALIC, false);

        var quality = map.quality();
        var starText = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i <= quality.ordinal()) {
                starText.append(quality.starSprite().fontChar());
            } else {
                starText.append(MapQuality.EMPTY_STAR.fontChar());
            }
        }

        var isParkour = map.settings().getVariant() == MapVariant.PARKOUR;
        var lore = new ArrayList<Component>();
        lore.add(Component.translatable("gui.play_maps.map_display.author", authorName));
        lore.add(Component.empty());
        var contentLine1 = Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(quality.tooltipBorderSprite().fontChar()).shadowColor(ShadowColor.none()))
                .append(Component.text(FontUtil.computeOffset(5)));
        if (isParkour) contentLine1 = contentLine1.append(map.getDifficultyComponent())
                .append(Component.text(FontUtil.computeOffset(6)));
        lore.add(contentLine1.append(getMapTypeComponent(map)));
        var difficultyIcon = map.getDifficulty().tooltipIcon();
        var totalPadding = Holder.QUALITY_BORDER_WIDTH - difficultyIcon.width();
        var leftPadding = (int) Math.ceil(totalPadding / 2.0) + (map.getDifficulty() == Difficulty.MEDIUM ? -1 : 0); // Cursed bias for medium :sob:
        var playsLikes = isParkour ? Holder.PLAYS_ICON_TEXT
                .append(Component.text(NumberUtil.formatCurrency(map.uniquePlays()), TextColor.color(0xaeaeae)))
                .append(Component.text(FontUtil.computeOffset(6))) : Component.empty();
        playsLikes = playsLikes
                .append(Holder.LIKES_ICON_TEXT)
                .append(Component.text(map.likes(), TextColor.color(0xaeaeae)));
        lore.add(Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(FontUtil.computeOffset(leftPadding) + difficultyIcon.fontChar() + FontUtil.computeOffset(totalPadding - leftPadding)).shadowColor(ShadowColor.none()))
                .append(Component.text(FontUtil.computeOffset(5)))
                .append(Component.text(starText.toString()))
                .append(Component.text(FontUtil.computeOffset(6)))
                .append(playsLikes));
        lore.add(Component.empty());

        var settingsLine = createSettingsLine(map);
        if (settingsLine != null) {
            lore.add(Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(BadSprite.require("icon/map_tooltip/settings").fontChar()).shadowColor(ShadowColor.none()))
                    .append(Component.text(FontUtil.computeOffset(2)))
                    .append(settingsLine));
            lore.add(Component.empty());
        }

        if (personalProgress != null) {
            var progress = personalProgress.getKey();
            var playtime = personalProgress.getValue();
            if (progress == PersonalizedMapData.Progress.COMPLETE) {
                lore.add(Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(BadSprite.require("icon/map_tooltip/completed").fontChar()).shadowColor(ShadowColor.none()))
                        .append(Component.text(FontUtil.computeOffset(6)))
                        .append(Component.translatable("gui.play_maps.map_display.completed", Component.text(NumberUtil.formatMapPlaytime(playtime, true)))));
                lore.add(Component.empty());
            } else if (progress == PersonalizedMapData.Progress.STARTED) {
                lore.add(Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.translatable("gui.play_maps.map_display.in_progress", Component.text(NumberUtil.formatMapPlaytime(playtime, true)))));
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
            if (!map.getSetting(setting)) continue;

            if (!components.isEmpty()) components.add(Holder.SEPARATOR);
            if (components.size() < 4) { // 4 = 2 separators & 2 settings
                components.add(Component.translatable("gui.play_maps.map_display." + setting.key()));
            } else enabledExtra++;
        }
        if (enabledExtra > 0) {
            components.add(Holder.SEPARATOR.append(Component.text("+" + enabledExtra)));
        }

        if (components.isEmpty()) return null;
        return Component.textOfChildren(components.toArray(new ComponentLike[0]));
    }

    private static @NotNull Component getMapTypeComponent(@NotNull MapData map) {
        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            return switch (map.settings().getParkourSubVariant()) {
                case SPEEDRUN -> Component.text("Speedrun Parkour", TextColor.color(0x55ffff));
                case SECTIONED -> Component.text("Sectioned Parkour", TextColor.color(0x55ffff));
                case RANKUP -> Component.text("Rankup Parkour", TextColor.color(0x55ffff));
                case GAUNTLET -> Component.text("Gauntlet Parkour", TextColor.color(0x55ffff));
                case DROPPER -> Component.text("Dropper Parkour", TextColor.color(0x55ffff));
                case ONE_JUMP -> Component.text("One Jump Parkour", TextColor.color(0x55ffff));
                case INFORMATIVE -> Component.text("Informative Parkour", TextColor.color(0x55ffff));
                case null -> Component.text("Generic Parkour", TextColor.color(0x55ffff));
            };
        } else if (map.settings().getVariant() == MapVariant.BUILDING) {
            return switch (map.settings().getBuildingSubVariant()) {
                case SHOWCASE -> Component.text("Building Showcase", TextColor.color(0x0B9F0B));
                case TUTORIAL -> Component.text("Building Tutorial", TextColor.color(0x0B9F0B));
                case null -> Component.text("Generic Building", TextColor.color(0x0B9F0B));
            };
        } else {
            return Component.text("Adventure Map", TextColor.color(0x9F0B0B));
        }
    }

    public static @NotNull Component rewriteWithQualityFont(@NotNull MapQuality quality, @NotNull String text) {
        class Holder {
            static final MiniMessage MM = MiniMessage.miniMessage();
        }

        //todo use values directly from placeholders/tx keys rather than duplicating here
        return switch (quality) {
            case UNRATED -> Component.text(text, TextColor.color(0xF2F2F2)); // placeholders.json5 -> white
            case GOOD -> Component.text(text, TextColor.color(0xF5DC3B)); // placeholders.json5 -> lemon
            case GREAT -> Component.text(text, TextColor.color(0x8CDB46)); // placeholders.json5 -> toxic_green
            case EXCELLENT ->
                    Holder.MM.deserialize(String.format("<gradient:#81AFFF:#8EB7FF:#9CBFFF:#A9C7FF:#B6CFFF:#A9C7FF:#9CBFFF:#8EB7FF:#81AFFF>%s</gradient>", text)); // gui.map_details.map_info_tab.quality.excellent.name
            case OUTSTANDING ->
                    Holder.MM.deserialize(String.format("<gradient:#78FFDF:#88FFE4:#98FFE9:#A8FFEE:#B8FFF3:#C8FFF8:#B8FFF3:#A8FFEE:#98FFE9:#88FFE4:#78FFDF>%s</gradient>", text)); // gui.map_details.map_info_tab.quality.outstanding.name;
            case MASTERPIECE ->
                    Holder.MM.deserialize(String.format("<gradient:#EE6EFF:#F17CFE:#F58BFE:#F899FD:#FCA8FD:#FFB6FC:#FCA8FD:#F899FD:#F58BFE:#F17CFE:#EE6EFF>%s</gradient>", text)); // gui.map_details.map_info_tab.quality.masterpiece.name;
        };
    }
}
