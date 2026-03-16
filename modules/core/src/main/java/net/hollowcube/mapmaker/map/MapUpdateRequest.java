package net.hollowcube.mapmaker.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.object.ObjectData;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RuntimeGson
public class MapUpdateRequest {
    public int protocolVersion = 0;
    public @Nullable String name = null;
    public @Nullable String icon = null;
    public @Nullable MapVariant variant = null;
    public @Nullable String subvariant = null;
    public @Nullable Pos spawnPoint = null;
    public @Nullable MapSize size = null;
    public @Nullable Boolean listed = null;

    public @Nullable Boolean onlySprint = null;
    public @Nullable Boolean noSprint = null;
    public @Nullable Boolean noJump = null;
    public @Nullable Boolean noSneak = null;
    public @Nullable Boolean boat = null;
    public @Nullable JsonObject extra = null;

    public @Nullable List<MapTags.Tag> tags = null;

    public List<ObjectData> newObjects = new ArrayList<>();
    public List<String> removedObjects = new ArrayList<>();

    public @Nullable MapQuality qualityOverride = null;

    public boolean hasChanges() {
        return name != null || icon != null || variant != null || subvariant != null || spawnPoint != null ||
               onlySprint != null || noSprint != null || noJump != null || noSneak != null || boat != null ||
               tags != null || !newObjects.isEmpty() || !removedObjects.isEmpty() || size != null ||
               qualityOverride != null || extra != null || protocolVersion != 0 || listed != null;
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

    public void setQualityOverride(@Nullable MapQuality qualityOverride) {
        this.qualityOverride = qualityOverride;
    }

    public void setExtraUpdate(String key, JsonElement value) {
        if (extra == null) extra = new JsonObject();
        extra.add(key, value);
    }
}
