package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PersonalizedMapData extends MapData {

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
