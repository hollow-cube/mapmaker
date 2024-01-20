package net.hollowcube.map.feature.play.effect;

import net.hollowcube.mapmaker.map.SaveState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public abstract class BaseEffectData {
    public static final int NO_RESET_HEIGHT = SaveState.PlayState.NO_RESET_HEIGHT;
    public static final int NO_TIME_LIMIT = 0;

    private String name;
    private int progressIndex;
    private int timeLimit;
    private int resetHeight;
    private boolean clearPotionEffects;
    private Map<PotionEffect, Integer> potionEffects;
    private Optional<Pos> teleport;
    //todo settings
    //todo items


    public BaseEffectData(
            String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            Map<PotionEffect, Integer> potionEffects,
            Optional<Pos> teleport
    ) {
        this.name = name;
        this.progressIndex = progressIndex;
        this.timeLimit = timeLimit;
        this.resetHeight = resetHeight;
        this.clearPotionEffects = clearPotionEffects;
        this.potionEffects = potionEffects;
        this.teleport = teleport;
    }

    public @NotNull String name() {
        return name.isEmpty() ? "Unnamed" : name;
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

    public @NotNull Map<PotionEffect, Integer> potionEffects() {
        return potionEffects;
    }

    public @NotNull Optional<Pos> teleport() {
        return teleport;
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
        this.teleport = Optional.ofNullable(teleport);
    }

    public void sendDebugInfo(@NotNull Player player) {
        player.sendMessage("Name: " + name());
        player.sendMessage("Progress index: " + (progressIndex() == -1 ? "none" : progressIndex()));
        player.sendMessage("Time limit: " + (timeLimit() == -1 ? "none" : timeLimit()));
        player.sendMessage("Reset height: " + (resetHeight() == NO_RESET_HEIGHT ? "inherited" : resetHeight()));
        player.sendMessage("Clear potion effects: " + clearPotionEffects());
        player.sendMessage("Potion effects: " + (potionEffects().isEmpty() ? "none" : potionEffects().toString()));
        player.sendMessage("Teleport: " + teleport().map(Point::toString).orElse("none"));
    }
}
