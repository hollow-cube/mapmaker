package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.map.setting.WeatherType;
import net.kyori.adventure.text.Component;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@RuntimeGson
public class MapSettings {
    public static final MapSetting<Boolean> BOAT = MapSetting.Embedded("boat", MapSettings::isBoat, MapUpdateRequest::setBoat);
    public static final MapSetting<Boolean> ONLY_SPRINT = MapSetting.Embedded("only_sprint", MapSettings::isOnlySprint, MapUpdateRequest::setOnlySprint);
    public static final MapSetting<Boolean> NO_SPRINT = MapSetting.Embedded("no_sprint", MapSettings::isNoSprint, MapUpdateRequest::setNoSprint);
    public static final MapSetting<Boolean> NO_JUMP = MapSetting.Embedded("no_jump", MapSettings::isNoJump, MapUpdateRequest::setNoJump);
    public static final MapSetting<Boolean> NO_SNEAK = MapSetting.Embedded("no_sneak", MapSettings::isNoSneak, MapUpdateRequest::setNoSneak);

    public static final MapSetting<Boolean> NO_SPECTATOR = MapSetting.Bool("no_spectator", false);
    public static final MapSetting<Boolean> RESET_IN_WATER = MapSetting.Bool("reset_in_water", false);
    public static final MapSetting<Boolean> RESET_IN_LAVA = MapSetting.Bool("reset_in_lava", false);
    public static final MapSetting<Boolean> NO_RELOG = MapSetting.Bool("no_relog", false);
    public static final MapSetting<Integer> TICK_RATE = MapSetting.Int("tick_rate", ServerFlag.SERVER_TICKS_PER_SECOND);

    public static final MapSetting<TimeOfDay> TIME_OF_DAY = MapSetting.Enum("time_of_day", TimeOfDay.NOON);
    public static final MapSetting<WeatherType> WEATHER_TYPE = MapSetting.Enum("weather_type", WeatherType.CLEAR);
    public static final MapSetting<Boolean> LIGHTING = MapSetting.Bool("lighting", false);

    public static final MapSetting<Boolean>[] TOOLTIP_SETTINGS = new MapSetting[]{
            BOAT, ONLY_SPRINT, NO_SPRINT, NO_JUMP, NO_SNEAK,
    };

    // Weird/one off/experimental settings
    public static final MapSetting<Boolean> PROGRESS_INDEX_ADDITION = MapSetting.Bool("progress_index_addition", false);

    transient MapUpdateRequest updates = new MapUpdateRequest();
    transient ReentrantLock updateLock = new ReentrantLock();

    private String name;
    private Material icon;
    private MapSize size;
    private MapVariant variant;
    private String subvariant;

    private Pos spawnPoint;

    private JsonObject extra;

    // Settings
    public enum SettingType {
        GAMEPLAY,
        VISUAL
    }

    public enum SettingValueType {
        BOOLEAN,
        ENUM
    }

    public enum Setting {
        // WARNING! Changing the variable names or order of these tags is dangerous.
        // This enum must match the order of the setting declarations in the GUI xml file.
        // See the warning in EditMap for more details.

        // Visual

        // Gameplay
        ONLYSPRINT(SettingType.GAMEPLAY, "Only Sprint", SettingValueType.BOOLEAN, null),
        NOSPRINT(SettingType.GAMEPLAY, "No Sprint", SettingValueType.BOOLEAN, null),
        NOJUMP(SettingType.GAMEPLAY, "No Jump", SettingValueType.BOOLEAN, null),
        NOSNEAK(SettingType.GAMEPLAY, "No Sneak", SettingValueType.BOOLEAN, null),

        NOSPEC(SettingType.GAMEPLAY, "No Spectator", SettingValueType.BOOLEAN, null),
        RESET_WATER(SettingType.GAMEPLAY, "Reset in Water", SettingValueType.BOOLEAN, null),
        RESET_LAVA(SettingType.GAMEPLAY, "Reset in Lava", SettingValueType.BOOLEAN, null),

        TIME_OF_DAY(SettingType.VISUAL, "Time of Day", SettingValueType.ENUM, TimeOfDay.class),
        WEATHER_TYPE(SettingType.VISUAL, "Weather", SettingValueType.ENUM, WeatherType.class),
        ;

        SettingType type;
        String name;
        SettingValueType valueType;
        Class<? extends Enum<?>> valueClass;

        Setting(SettingType type, String name, SettingValueType valueType, Class<? extends Enum<?>> valueClass) {
            this.type = type;
            this.name = name;
            this.valueType = valueType;
            this.valueClass = valueClass;
        }

        public String displayName() {
            return this.name;
        }

        public SettingType getType() {
            return this.type;
        }

        public SettingValueType getValueType() {
            return this.valueType;
        }

        public Class<? extends Enum<?>> getValueClass() {
            return this.valueClass;
        }
    }

    private boolean onlySprint = false;
    private boolean noSprint = false;
    private boolean noJump = false;
    private boolean noSneak = false;
    private boolean boat = false;

    private List<MapTags.Tag> tags;

    public MapSettings() {
        this.name = "";
        this.icon = null;
        this.variant = MapVariant.BUILDING;
        this.size = MapSize.NORMAL;
        this.spawnPoint = new Pos(0.5, 40, 0.5);
        this.tags = new ArrayList<>();
    }

    public MapSettings(
            @Nullable String name,
            @Nullable Material icon,
            @Nullable MapSize size,
            @Nullable MapVariant variant,
            @Nullable Pos spawnPoint,
            boolean onlySprint,
            boolean noSprint,
            boolean noJump,
            boolean noSneak,
            @Nullable List<MapTags.Tag> tags
    ) {
        this.name = name;
        this.icon = icon;
        this.size = size;
        this.variant = variant;
        this.spawnPoint = spawnPoint;
        this.onlySprint = onlySprint;
        this.noSprint = noSprint;
        this.noJump = noJump;
        this.noSneak = noSneak;
        this.tags = tags;
    }

    /**
     * Run a callback with the update request, and if the callback returns true, reset the update request.
     */
    public void withUpdateRequest(@NotNull Function<@NotNull MapUpdateRequest, Boolean> callback) {
        updateLock.lock();
        try {
            if (updates.hasChanges() && callback.apply(updates)) {
                this.updates = new MapUpdateRequest();
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Run a callback with the update request, and if the callback returns true, reset the update request.
     */
    public void modifyUpdateRequest(@NotNull Consumer<@NotNull MapUpdateRequest> callback) {
        updateLock.lock();
        try {
            callback.accept(updates);
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getNameSafe() {
        if (name == null || name.isEmpty())
            return MapData.DEFAULT_NAME;
        return name;
    }

    public @NotNull Component getNameComponent() {
        return Component.text(getNameSafe());
    }

    public @NotNull Component getTagsComponent() {
        var tags = getTags();
        var maxTags = Math.min(2, tags.size());
        var currTagIdx = 0;
        var tagsLabel = Component.text("");
        // TODO find a way to terminate after a certain length of displayed text
        while (currTagIdx < maxTags) {
//            var tagName = tags.get(currTagIdx).displayName(); // TODO figure out how to get translation key working
            var tagName = Component.text(tags.get(currTagIdx).name());
            tagsLabel = tagsLabel.append(tagName);
            tagsLabel = tagsLabel.append(Component.text(", "));
            currTagIdx++;
        }
        if (currTagIdx == 0)
            tagsLabel = Component.text("None");
        if (currTagIdx < tags.size())
            tagsLabel = tagsLabel.append(Component.text(String.format("+%s", tags.size() - currTagIdx)));
        return tagsLabel;
    }

    public @Nullable String getTagsString() {
        List<String> tags = getTags().stream().map(tag -> tag.name).collect(Collectors.toList());

        if (tags.isEmpty()) {
            return null;
        }

        var tagsLength = FontUtil.measureText(String.join(", ", tags));
        var maxLength = 139;

        var initialTagsCount = tags.size();

        while (tagsLength > maxLength && !tags.isEmpty()) {
            tags.remove(tags.size() - 1);
            tagsLength = FontUtil.measureText(String.join(", ", tags));
        }

        StringBuilder stringBuilder = new StringBuilder();
        int removedTagsCount = initialTagsCount - tags.size();

        for (int i = 0; i < tags.size(); i++) {
            String tagsName = tags.get(i);
            stringBuilder.append(tagsName);
            if (i < tags.size() - 1) {
                stringBuilder.append(", ");
            } else if (i == tags.size() - 1 && removedTagsCount > 0) {
                stringBuilder.append(", +").append(removedTagsCount);
            }
        }

        return stringBuilder.toString();
    }

    public String getTagsFullString() {
        List<String> tags = getTags().stream().map(tag -> tag.name).toList();

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < tags.size(); i++) {
            String tagsName = tags.get(i);
            stringBuilder.append(tagsName);
            if (i < tags.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
    }

    public @Nullable String getSettingsString() {
        List<String> enabledSettings = new ArrayList<>();

        if (isOnlySprint()) {
            enabledSettings.add("Only Sprint");
        }
        if (isNoSprint()) {
            enabledSettings.add("No Sprint");
        }
        if (isNoJump()) {
            enabledSettings.add("No Jump");
        }
        if (isNoSneak()) {
            enabledSettings.add("No Sneak");
        }
        if (isBoat()) {
            enabledSettings.add("Boats");
        }

        if (enabledSettings.isEmpty()) {
            return null;
        }

        var initialSettingCount = enabledSettings.size();
        var settingsLength = FontUtil.measureText(String.join(", ", enabledSettings));
        var maxLength = 139;

        while (settingsLength > maxLength && !enabledSettings.isEmpty()) {
            enabledSettings.remove(enabledSettings.size() - 1);
            settingsLength = FontUtil.measureText(String.join(", ", enabledSettings));
        }

        StringBuilder stringBuilder = new StringBuilder();
        int removedSettingCount = initialSettingCount - enabledSettings.size();

        for (int i = 0; i < enabledSettings.size(); i++) {
            String settingName = enabledSettings.get(i);
            stringBuilder.append(settingName);
            if (i < enabledSettings.size() - 1) {
                stringBuilder.append(", ");
            } else if (i == enabledSettings.size() - 1 && removedSettingCount > 0) {
                stringBuilder.append(", +").append(removedSettingCount);
            }
        }

        return stringBuilder.toString();
    }

    public String getSettingsFullString() {
        List<String> enabledSettings = new ArrayList<>();

        if (isOnlySprint()) {
            enabledSettings.add("Only Sprint");
        }
        if (isNoSprint()) {
            enabledSettings.add("No Sprint");
        }
        if (isNoJump()) {
            enabledSettings.add("No Jump");
        }
        if (isNoSneak()) {
            enabledSettings.add("No Sneak");
        }
        if (isBoat()) {
            enabledSettings.add("Boats");
        }

        if (enabledSettings.isEmpty()) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < enabledSettings.size(); i++) {
            String settingName = enabledSettings.get(i);
            stringBuilder.append(settingName);
            if (i < enabledSettings.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
    }

    public void setName(@NotNull String name) {
        updateLock.lock();
        try {
            updates.setName(name);
            this.name = name;
        } finally {
            updateLock.unlock();
        }
    }

    public @Nullable Material getIcon() {
        return icon;
    }

    public void setIcon(@NotNull Material icon) {
        updateLock.lock();
        try {
            updates.setIcon(icon.name());
            this.icon = icon;
        } finally {
            updateLock.unlock();
        }
    }

    public @Nullable MapSize getSize() {
        return size;
    }

    public void setSize(@NotNull MapSize size) {
        updateLock.lock();
        try {
            updates.setSize(size);
            this.size = size;
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull MapVariant getVariant() {
        return variant;
    }

    public void setVariant(@NotNull MapVariant type) {
        updateLock.lock();
        try {
            updates.setVariant(type);
            this.variant = type;
        } finally {
            updateLock.unlock();
        }
    }

    public @Nullable ParkourSubVariant getParkourSubVariant() {
        if (subvariant == null) return null;
        try {
            return ParkourSubVariant.valueOf(subvariant.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable BuildingSubVariant getBuildingSubVariant() {
        if (subvariant == null) return null;
        try {
            return BuildingSubVariant.valueOf(subvariant.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setParkourSubVariant(@Nullable ParkourSubVariant subvariant) {
        updateLock.lock();
        try {
            Check.argCondition(variant != MapVariant.PARKOUR, "Parkour subvariant can only be set for parkour maps");
            this.subvariant = subvariant == null ? null : subvariant.name().toLowerCase();
            updates.setSubVariant(this.subvariant);
        } finally {
            updateLock.unlock();
        }
    }

    public void setBuildingSubVariant(@Nullable BuildingSubVariant subvariant) {
        updateLock.lock();
        try {
            Check.argCondition(variant == MapVariant.PARKOUR, "Building subvariant can only be set for building maps");
            this.subvariant = subvariant == null ? null : subvariant.name().toLowerCase();
            updates.setSubVariant(this.subvariant);
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull Pos getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(@NotNull Pos spawnPoint) {
        updateLock.lock();
        try {
            updates.setSpawnPoint(spawnPoint);
            this.spawnPoint = spawnPoint;
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isOnlySprint() {
        return onlySprint;
    }

    public void setOnlySprint(boolean onlySprint) {
        updateLock.lock();
        try {
            updates.setOnlySprint(onlySprint);
            this.onlySprint = onlySprint;
            if (onlySprint)
                setNoSprint(false);
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isNoSprint() {
        return noSprint;
    }

    public void setNoSprint(boolean noSprint) {
        updateLock.lock();
        try {
            updates.setNoSprint(noSprint);
            this.noSprint = noSprint;
            if (noSprint)
                setOnlySprint(false);
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isNoJump() {
        return noJump;
    }

    public void setNoJump(boolean noJump) {
        updateLock.lock();
        try {
            updates.setNoJump(noJump);
            this.noJump = noJump;
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isNoSneak() {
        return noSneak;
    }

    public void setNoSneak(boolean noSneak) {
        updateLock.lock();
        try {
            updates.setNoSneak(noSneak);
            this.noSneak = noSneak;
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isBoat() {
        return boat;
    }

    public void setBoat(boolean boat) {
        updateLock.lock();
        try {
            updates.setBoat(boat);
            this.boat = boat;
        } finally {
            updateLock.unlock();
        }
    }

    public List<MapTags.Tag> getTags() {
        if (this.tags == null)
            return new ArrayList<>();
        return this.tags;
    }

    public boolean addTag(@NotNull MapTags.Tag tag) {
        Check.stateCondition(variant == MapVariant.BUILDING && tag.type == MapTags.TagType.GAMEPLAY,
                "building maps may not have gameplay tags");

        updateLock.lock();
        try {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            } else if (this.tags.contains(tag)) {
                return false; // Already present
            }

            this.tags.add(tag);
            updates.setTags(this.tags);
            return true;
        } finally {
            updateLock.unlock();
        }
    }

    public boolean removeTag(@NotNull MapTags.Tag tag) {
        updateLock.lock();
        try {
            if (this.tags == null) return false;
            var removed = this.tags.remove(tag);
            if (removed) updates.setTags(this.tags);
            return removed;
        } catch (UnsupportedOperationException ignored) {
            // todo: Awful awful awful why is this ending up immutable (@nix >:( )
            this.tags = new ArrayList<>(this.tags);
            return removeTag(tag);
        } finally {
            updateLock.unlock();
        }
    }

    public void removeGameplayTags() {
        updateLock.lock();
        try {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags = new ArrayList<>(this.tags.stream().filter(
                    tag -> tag.getType() == MapTags.TagType.VISUAL
            ).toList());
        } finally {
            updateLock.unlock();
        }
    }

    public void removeVisualTags() {
        updateLock.lock();
        try {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags = new ArrayList<>(this.tags.stream().filter(
                    tag -> tag.getType() == MapTags.TagType.GAMEPLAY
            ).toList());
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull JsonObject extra() {
        if (this.extra == null) this.extra = new JsonObject();
        return this.extra;
    }
}
