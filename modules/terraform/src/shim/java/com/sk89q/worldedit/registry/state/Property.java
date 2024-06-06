package com.sk89q.worldedit.registry.state;

public interface Property<T> {

    String getKey();

    String serialize(T value);

    T deserialize(String value);
}
