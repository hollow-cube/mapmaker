package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class PersonalizedMapData extends MapData {

    public PersonalizedMapData() {
        super();
    }

    public PersonalizedMapData(@NotNull String id, @NotNull String owner, @NotNull MapSettings settings, long publishedId, @Nullable Instant publishedAt) {
        super(id, owner, settings, publishedId, publishedAt);
    }

    public enum Progress {
        @SerializedName("none") NONE,
        @SerializedName("started") STARTED,
        @SerializedName("complete") COMPLETE
    }
    private Progress progress;

    public @NotNull Component getCompletionStateText() {
        return Component.translatable(switch (progress) {
            case NONE -> "gui.play_maps.map_display.progress_none";
            case STARTED -> "gui.play_maps.map_display.progress_started";
            case COMPLETE -> "gui.play_maps.map_display.progress_complete";
        });
    }

}
