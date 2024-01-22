package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class SaveStateUpdateRequest {
    private Boolean completed = null;
    private Long playtime = null;

    @SerializedName("editState")
    private SaveState.BuildState buildState = null;
    private SaveState.PlayState playState = null;

    public boolean hasChanges() {
        return completed != null || playtime != null || buildState != null || playState != null;
    }

    public @NotNull SaveStateUpdateRequest setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPlaytime(long playtime) {
        this.playtime = playtime;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setBuildState(@NotNull SaveState.BuildState buildState) {
        this.buildState = buildState;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPlayState(@NotNull SaveState.PlayState playState) {
        this.playState = playState;
        return this;
    }

}
