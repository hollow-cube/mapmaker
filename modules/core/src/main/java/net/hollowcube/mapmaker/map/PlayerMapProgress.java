package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record PlayerMapProgress(@NotNull String mapId, @NotNull PlayerMapProgress.Progress progress, int playtime) {

    public enum Progress {
        @SerializedName("none") NONE,
        @SerializedName("started") STARTED,
        @SerializedName("complete") COMPLETE
    }
}
