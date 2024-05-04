package net.hollowcube.mapmaker.map;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.object.ObjectData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    public static final int MIN_PLAYS_FOR_DIFFICULTY = 10;

    public static final String SPAWN_MAP_ID = System.getenv("MAPMAKER_SPAWN_MAP_ID");

    private String id;
    private String owner;
    private MapSettings settings;
    private MapVerification verification = MapVerification.UNVERIFIED;

    private long publishedId;
    private Instant publishedAt;

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
        return setting.read(settings);
    }

    public <T> void setSetting(@NotNull MapSetting<T> setting, @NotNull T value) {
        setting.write(settings, value);
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

    public int uniquePlays() {
        return uniquePlays;
    }

    public double clearRate() {
        return clearRate;
    }

    public @NotNull Component getDifficultyComponent() {
        if (uniquePlays() < MIN_PLAYS_FOR_DIFFICULTY)
            return Component.translatable("gui.play_maps.map_display.difficulty.unknown");

        return Component.translatable(
                "gui.play_maps.map_display.difficulty." + getDifficultyName(),
                Component.text(getClearRateString())
        );
    }

    public @NotNull String getDifficultyName() {
        var cr = clearRate();
        if (cr < 0.05) return "nightmare";
        if (cr < 0.25) return "expert";
        if (cr < 0.5) return "hard";
        if (cr < 0.75) return "medium";
        return "easy";
    }

    public @NotNull String getClearRateString() {
        var cr = clearRate() * 100;
        if (cr >= 100) return "100";
        else if (cr <= 0) return "0";
        else if (cr >= 10) return String.format("%.1f", cr);
        else if (cr >= 1) return String.format("%.2f", cr);
        else return String.format("%.3f", cr);
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
            if (objectUsage() + object.type().cost() > objectLimit)
                return false;

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


    /**
     * Returns a component with the map name and a hover text that shows the map details GUI basically.
     * <p>
     * If the map is published it will also have a join link
     *
     * @return
     */
    public static @NotNull Component createHeadlessComponent(@NotNull MapData map, @NotNull PlayerService playerService) {
        var lore = new ArrayList<Component>();
        lore.add(Component.text(map.name()));
        var authorName = playerService.getPlayerDisplayName2(map.owner());
        lore.add(Component.translatable("gui.play_maps.map_display.author", authorName.build()));
        lore.add(Component.empty());
        lore.add(Component.translatable("gui.play_maps.map_display.type", getMapTypeComponent(map)));
        if (map.settings().getVariant() == MapVariant.PARKOUR)
            lore.add(Component.translatable("gui.play_maps.map_display.difficulty", map.getDifficultyComponent()));
        if (map.quality() == MapQuality.GOOD) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.good"));
        } else if (map.quality() == MapQuality.GREAT) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.great"));
        } else if (map.quality() == MapQuality.EXCELLENT) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.excellent"));
        } else if (map.quality() == MapQuality.OUTSTANDING) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.outstanding"));
        } else if (map.quality() == MapQuality.MASTERPIECE) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.masterpiece"));
        } else {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.unrated"));
        }

        lore.add(Component.translatable("gui.play_maps.map_display.id", Component.text(map.publishedIdString())));
        var tags = map.settings().getTags();
        if (!tags.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.translatable("gui.play_maps.map_display.tags_header"));
            for (var tag : tags) {
                lore.add(Component.translatable("gui.play_maps.map_display.tags_single", Component.text(tag.displayName())));
            }
        }

        if (map.settings().isOnlySprint() || map.settings().isNoSprint() || map.settings().isNoJump()
                || map.settings().isNoSneak() || map.settings().isBoat()) {
            lore.add(Component.empty());
            lore.add(Component.translatable("gui.play_maps.map_display.settings_header"));

            int settingsCount = 0;
            int extraSettingsCount = 0;

            if (map.settings().isOnlySprint()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("Only Sprint")));
                settingsCount++;
            }
            if (map.settings().isNoSprint()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Sprint")));
                settingsCount++;
            }
            if (map.settings().isNoJump()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Jump")));
                settingsCount++;
            }
            if (map.settings().isNoSneak()) {
                if (settingsCount < 3) {
                    lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Sneak")));
                } else {
                    extraSettingsCount++;
                }
                settingsCount++;
            }
            if (map.settings().isBoat()) {
                if (settingsCount < 3) {
                    lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("Boats")));
                } else {
                    extraSettingsCount++;
                }
                settingsCount++;
            }

            if (settingsCount > 3) {
                lore.add(Component.translatable("gui.play_maps.map_display.setting_more", Component.text(extraSettingsCount)));
            }
        }

        lore.add(Component.empty());
        lore.addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display_headless.footer", List.of()));

        var builder = Component.text();
        for (int i = 0; i < lore.size(); i++) {
            builder.append(LanguageProviderV2.translate(lore.get(i)));
            if (i < lore.size() - 1) {
                builder.appendNewline();
            }
        }

        var comp = Component.text(map.name(), TextColor.color(0x15ADD3));
        if (map.isPublished()) {
            comp = comp.hoverEvent(HoverEvent.showText(builder.build()))
                    .clickEvent(ClickEvent.runCommand("/play " + MapData.formatPublishedId(map.publishedId())));
        }
//        else {
//            var hoverText = Component.text("Click to view details!")
//                    .appendNewline()
//                    .append(Component.text("LINE 2"));
//            comp = comp.hoverEvent(HoverEvent.showText(hoverText))
//                    .clickEvent(ClickEvent.runCommand("/map details " + map.id()));
//        }

        return comp;
//        return builder.build();
    }

    private static @NotNull Component getMapTypeComponent(@NotNull MapData map) {
        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            return switch (map.settings().getParkourSubVariant()) {
                case SPEEDRUN -> Component.text("Speedrun Parkour", TextColor.color(0x15ADD3));
                case SECTIONED -> Component.text("Sectioned Parkour", TextColor.color(0x15ADD3));
                case RANKUP -> Component.text("Rankup Parkour", TextColor.color(0x15ADD3));
                case GAUNTLET -> Component.text("Gauntlet Parkour", TextColor.color(0x15ADD3));
                case DROPPER -> Component.text("Dropper Parkour", TextColor.color(0x15ADD3));
                case ONE_JUMP -> Component.text("One Jump Parkour", TextColor.color(0x15ADD3));
                case INFORMATIVE -> Component.text("Informative Parkour", TextColor.color(0x15ADD3));
                case null -> Component.text("Generic Parkour", TextColor.color(0x15ADD3));
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
}
