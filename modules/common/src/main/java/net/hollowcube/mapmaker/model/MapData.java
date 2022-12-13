package net.hollowcube.mapmaker.model;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MapData {

    private String id;
    private String owner;
    private String name;

    // ID of the file in storage, or null if the map does not yet exist (it is lazily created)
    private String mapFileId;
    private List<POI> pois = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMapFileId() {
        return mapFileId;
    }

    public void setMapFileId(String mapFileId) {
        this.mapFileId = mapFileId;
    }

    public List<POI> getPois() {
        return pois;
    }

    public void addPOI(@NotNull POI poi) {
        pois.add(poi);
    }

    public void removePOI(@NotNull Point pos) {
        pois.removeIf(poi -> poi.pos.equals(pos));
    }

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public record POI(String type, Point pos) {}

}
