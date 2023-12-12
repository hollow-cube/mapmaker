package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class PersonalizedMapData extends MapData {

    public static final int MIN_PLAYS_FOR_DIFFICULTY = 10;

    public enum Progress {
        @SerializedName("none") NONE,
        @SerializedName("started") STARTED,
        @SerializedName("complete") COMPLETE
    }
    private Progress progress;

    private int uniquePlays;
    private double clearRate;

    public PersonalizedMapData() {
        super();
    }

    public PersonalizedMapData(@NotNull String id, @NotNull String owner, @NotNull MapSettings settings, long publishedId, @Nullable Instant publishedAt) {
        super(id, owner, settings, publishedId, publishedAt);
    }

    public PersonalizedMapData(@NotNull MapData map, Progress progress) {
        super(map.id(), map.owner(), map.settings(), map.publishedId(), map.publishedAt());
        this.progress = progress;
    }

    public @NotNull Component getCompletionStateText() {
        return Component.translatable(switch (progress) {
            case NONE -> "gui.play_maps.map_display.progress_none";
            case STARTED -> "gui.play_maps.map_display.progress_started";
            case COMPLETE -> "gui.play_maps.map_display.progress_complete";
        });
    }

    public @NotNull Component getDifficultyComponent() {
        if (getUniquePlays() < PersonalizedMapData.MIN_PLAYS_FOR_DIFFICULTY)
            return Component.translatable("gui.play_maps.map_display.difficulty.unknown");

        return Component.translatable(
                "gui.play_maps.map_display.difficulty." + getDifficultyName(),
                Component.text(getClearRateString())
        );
    }

    public @NotNull String getDifficultyName() {
        var cr = getClearRate();
        if (cr < 0.05) return "nightmare";
        if (cr < 0.25) return "expert";
        if (cr < 0.5) return "hard";
        if (cr < 0.75) return "medium";
        return "easy";
    }

    public @NotNull String getClearRateString() {
        var cr = getClearRate() * 100;
        if (cr >= 100) return "100";
        else if (cr <= 0) return "0";
        else if (cr >= 10) return String.format("%.1f", cr);
        else if (cr >= 1) return String.format("%.2f", cr);
        else return String.format("%.3f", cr);
    }

    public int getUniquePlays() {
        return uniquePlays;
    }

    public double getClearRate() {
        return clearRate;
    }
}
