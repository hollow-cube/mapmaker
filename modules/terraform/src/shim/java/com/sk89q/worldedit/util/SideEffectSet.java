package com.sk89q.worldedit.util;

import java.util.HashMap;
import java.util.Map;

public record SideEffectSet(
        Map<SideEffect, SideEffect.State> entries
) {

    public SideEffectSet() {
        this(Map.of());
    }

    public SideEffectSet with(SideEffect sideEffect, SideEffect.State state) {
        var newState = new HashMap<>(entries);
        newState.put(sideEffect, state);
        return new SideEffectSet(Map.copyOf(newState));
    }
}
