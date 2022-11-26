package net.hollowcube.mapmaker.model;

import org.jetbrains.annotations.NotNull;

public class MapData {

    public enum Type {
        BUILD_SHOWCASE
    }

    private String id;
    private Type type;
    private String name;

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

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
