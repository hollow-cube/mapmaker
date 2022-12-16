package net.hollowcube.mapmaker.model;

public class MapData {

    public enum Type {
        BUILD_SHOWCASE
    }

    private String id;
    private Type type;
    private String name;

    // ID of the file in storage, or null if the map does not yet exist (it is lazily created)
    private String mapFileId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
