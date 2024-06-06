package com.sk89q.worldedit.registry.state;

import java.util.List;

public class IntegerProperty implements Property<Integer> {

    private String name;

    public IntegerProperty(String name, List<Integer> values) {
        this.name = name;
    }

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public String serialize(Integer value) {
        return String.valueOf(Math.max(1, value));//todo why is W/E setting layers to zero
    }

    @Override
    public Integer deserialize(String value) {
        return Integer.parseInt(value);
    }
}
