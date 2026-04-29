package net.hollowcube.luau.gen.docs;

/// JSON form of a `@LuaProperty`. At least one of `getter` / `setter` is non-null. The
/// resolver later cross-validates that getter and setter types match.
public record RawProperty(
    String luaName,
    RawGetter getter,
    RawSetter setter
) {}
