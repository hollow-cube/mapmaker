package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.object.ObjectData;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MapUpdateRequest {
    String name = null;
    String icon = null;
    MapVariant variant = null;
    String subvariant = null;
    Pos spawnPoint = null;

    Boolean onlySprint = null;
    Boolean noSprint = null;
    Boolean noJump = null;
    Boolean noSneak = null;
    Boolean boat = null;

    List<MapTags.Tag> tags = null;

    List<ObjectData> newObjects = new ArrayList<>();
    List<String> removedObjects = new ArrayList<>();

    public boolean hasChanges() {
        return name != null || icon != null || variant != null || subvariant != null || spawnPoint != null ||
                onlySprint != null || noSprint != null || noJump != null || noSneak != null || boat != null ||
                tags != null || !newObjects.isEmpty() || !removedObjects.isEmpty();
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

    public void setOnlySprint(boolean onlySprint) { this.onlySprint = onlySprint; }
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
}
