package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public abstract class BaseEffectData {
    public static final int NO_RESET_HEIGHT = PlayState.NO_RESET_HEIGHT;
    public static final int NO_TIME_LIMIT = 0;

    // Used for setting coords on plates
    // Either an Entity or a Point
    public static final Tag<Object> TARGET_PLATE = Tag.Transient("custom_blocks/tp_plate/target");

    private String name;
    private int progressIndex;
    private int timeLimit;
    private int resetHeight;
    private boolean clearPotionEffects;
    private final PotionEffectList potionEffects;
    private Pos teleport;
    private HotbarItems items;
    private final SavedMapSettings settings;

    public BaseEffectData(
            @NotNull String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            @NotNull PotionEffectList potionEffects,
            @Nullable Pos teleport, @NotNull HotbarItems items,
            @Nullable SavedMapSettings settings
    ) {
        this.name = name;
        this.progressIndex = progressIndex;
        this.timeLimit = timeLimit;
        this.resetHeight = resetHeight;
        this.clearPotionEffects = clearPotionEffects;
        this.potionEffects = potionEffects;
        this.teleport = teleport;
        this.items = items;
        this.settings = Objects.requireNonNullElseGet(settings, SavedMapSettings::new);
    }

    public @NotNull String displayName() {
        return name.isEmpty() ? "Unnamed" : name;
    }

    public @NotNull String name() {
        return name;
    }

    public boolean hasName() {
        return !name.isEmpty();
    }

    public int progressIndex() {
        return progressIndex;
    }

    public int timeLimit() {
        return timeLimit;
    }

    public int resetHeight() {
        return resetHeight;
    }

    public boolean clearPotionEffects() {
        return clearPotionEffects;
    }

    public @NotNull PotionEffectList potionEffects() {
        return potionEffects;
    }

    @NotNull
    Optional<PotionEffectList> optPotionEffects() {
        return Optional.of(potionEffects);
    }

    public @Nullable Pos teleport() {
        return teleport;
    }

    public @NotNull HotbarItems items() {
        return items;
    }

    public SavedMapSettings settings() {
        return settings;
    }

    public void setName(@Nullable String name) {
        this.name = name == null ? "" : name;
    }

    public void setProgressIndex(int progressIndex) {
        this.progressIndex = progressIndex;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setResetHeight(int resetHeight) {
        this.resetHeight = resetHeight;
    }

    public void setClearPotionEffects(boolean clearPotionEffects) {
        this.clearPotionEffects = clearPotionEffects;
    }

    public void setTeleport(@Nullable Pos teleport) {
        this.teleport = teleport;
    }

    public void setItems(@NotNull HotbarItems items) {
        this.items = items;
    }

    public void sendDebugInfo(@NotNull Player player) {
        player.sendMessage("Name: " + displayName());
        player.sendMessage("Progress index: " + (progressIndex() == -1 ? "none" : progressIndex()));
        player.sendMessage("Time limit: " + (timeLimit() == -1 ? "none" : timeLimit()));
        player.sendMessage("Reset height: " + (resetHeight() == NO_RESET_HEIGHT ? "inherited" : resetHeight()));
        player.sendMessage("Clear potion effects: " + clearPotionEffects());
        player.sendMessage("Potion effects: " + (potionEffects().isEmpty() ? "none" : potionEffects().toString()));
        player.sendMessage("Teleport: " + teleport());
        player.sendMessage("Items: " + items());
        player.sendMessage("Settings: " + settings());
    }
}
