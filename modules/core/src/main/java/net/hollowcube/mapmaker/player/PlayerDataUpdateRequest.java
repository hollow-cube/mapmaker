package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class PlayerDataUpdateRequest {
    private String username = null;
    private List<String> ipHistory = null;
    private Instant lastOnline = null;

    @SerializedName("settings_updates")
    private JsonObject settings = null;

    public boolean hasChanges() {
        return username != null || ipHistory != null || lastOnline != null || settings != null;
    }

    public @NotNull PlayerDataUpdateRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setIpHistory(List<String> ipHistory) {
        this.ipHistory = ipHistory;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setLastOnline(Instant lastOnline) {
        this.lastOnline = lastOnline;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest updateSetting(@NotNull String key, @NotNull JsonElement value) {
        if (settings == null) {
            settings = new JsonObject();
        }
        settings.add(key, value);
        return this;
    }

}
