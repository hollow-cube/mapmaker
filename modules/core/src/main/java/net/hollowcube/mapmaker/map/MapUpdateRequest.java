package net.hollowcube.mapmaker.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.object.ObjectData;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MapUpdateRequest {
    public String name = null;
    public String icon = null;
    public MapVariant variant = null;
    public String subvariant = null;
    public Pos spawnPoint = null;
    public MapSize size = null;

    public Boolean onlySprint = null;
    public Boolean noSprint = null;
    public Boolean noJump = null;
    public Boolean noSneak = null;
    public Boolean boat = null;
    public JsonObject extra = null;

    public List<MapTags.Tag> tags = null;

    public List<ObjectData> newObjects = new ArrayList<>();
    public List<String> removedObjects = new ArrayList<>();

    public MapQuality qualityOverride = null;


    public boolean hasChanges() {
        return name != null || icon != null || variant != null || subvariant != null || spawnPoint != null ||
                onlySprint != null || noSprint != null || noJump != null || noSneak != null || boat != null ||
                tags != null || !newObjects.isEmpty() || !removedObjects.isEmpty() || size != null ||
                qualityOverride != null || extra != null;
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

    public void setExtraUpdate(@NotNull String key, @NotNull JsonElement value) {
        if (extra == null) extra = new JsonObject();
        extra.add(key, value);
    }
}
