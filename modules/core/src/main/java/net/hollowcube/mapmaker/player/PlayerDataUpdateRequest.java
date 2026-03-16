package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

@RuntimeGson
public class PlayerDataUpdateRequest {
    private @Nullable String username = null;
    private @Nullable List<String> ipHistory = null;
    private @Nullable Instant lastOnline = null;

    @SerializedName("settingsUpdates")
    private @Nullable JsonObject settings = null;

    private @Nullable JsonObject cosmetics = null;

    public boolean hasChanges() {
        return username != null || ipHistory != null || lastOnline != null || settings != null || cosmetics != null;
    }

    public PlayerDataUpdateRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public PlayerDataUpdateRequest setIpHistory(List<String> ipHistory) {
        this.ipHistory = ipHistory;
        return this;
    }

    public PlayerDataUpdateRequest setLastOnline(Instant lastOnline) {
        this.lastOnline = lastOnline;
        return this;
    }

    public PlayerDataUpdateRequest updateSetting(String key, JsonElement value) {
        if (settings == null) {
            settings = new JsonObject();
        }
        settings.add(key, value);
        return this;
    }

    public PlayerDataUpdateRequest updateCosmetic(CosmeticType type, @Nullable String id) {
        if (cosmetics == null) {
            cosmetics = new JsonObject();
        }
        cosmetics.add(type.id(), id == null ? JsonNull.INSTANCE : new JsonPrimitive(id));
        return this;
    }

    public @Nullable JsonObject settings() {
        return settings == null || settings.isEmpty() ? null : settings;
    }

}
