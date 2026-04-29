package net.hollowcube.luau.slopgen.model;

import org.jetbrains.annotations.Nullable;

/// A single Lua-visible property. At least one of `getter` / `setter` is non-null.
public record PropertySpec(
    String luaName,
    @Nullable AccessorSpec getter,
    @Nullable AccessorSpec setter
) {
    public PropertySpec {
        if (getter == null && setter == null)
            throw new IllegalArgumentException("Property '" + luaName + "' has neither getter nor setter");
    }
}
