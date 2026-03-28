package net.hollowcube.mapmaker.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public class MapUpdateRequest {
    public int protocolVersion = 0;
    public String name = null;
    public String icon = null;
    public MapVariant variant = null;
    public String subvariant = null;
    public Pos spawnPoint = null;
    public MapSize size = null;
    public Boolean listed = null;

    public Boolean onlySprint = null;
    public Boolean noSprint = null;
    public Boolean noJump = null;
    public Boolean noSneak = null;
    public Boolean boat = null;
    public JsonObject extra = null;

    public List<MapTags.Tag> tags = null;

    public Leaderboard leaderboard = null;

    public MapQuality qualityOverride = null;

    public boolean hasChanges() {
        return name != null || icon != null || variant != null || subvariant != null || spawnPoint != null ||
               onlySprint != null || noSprint != null || noJump != null || noSneak != null || boat != null ||
               tags != null || size != null || qualityOverride != null || extra != null || protocolVersion != 0 ||
               listed != null || leaderboard != null;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void setIcon(@Nullable String icon) {
        this.icon = icon;
    }

    public void setVariant(@Nullable MapVariant variant) {
        this.variant = variant;
    }

    public void setSubVariant(@Nullable String subvariant) {
        this.subvariant = subvariant == null ? "none" : subvariant;
    }

    public void setSpawnPoint(@Nullable Pos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public void setSize(@Nullable MapSize size) {
        this.size = size;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
    }

    public void setOnlySprint(boolean onlySprint) {
        this.onlySprint = onlySprint;
    }

    public void setNoSprint(boolean noSprint) {
        this.noSprint = noSprint;
    }

    public void setNoJump(boolean noJump) {
        this.noJump = noJump;
    }

    public void setNoSneak(boolean noSneak) {
        this.noSneak = noSneak;
    }

    public void setBoat(Boolean boat) {
        this.boat = boat;
    }

    public void setTags(List<MapTags.Tag> tags) {
        this.tags = tags;
    }

    public void setLeaderboard(@NotNull Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    public void setQualityOverride(@Nullable MapQuality qualityOverride) {
        this.qualityOverride = qualityOverride;
    }

    public void setExtraUpdate(@NotNull String key, @NotNull JsonElement value) {
        if (extra == null) extra = new JsonObject();
        extra.add(key, value);
    }
}
