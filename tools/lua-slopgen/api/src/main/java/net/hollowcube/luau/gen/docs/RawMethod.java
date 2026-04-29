package net.hollowcube.luau.gen.docs;

import java.util.List;

/// JSON form of a `@LuaMethod`. `returns` is a list of raw type-expression strings; multiple
/// entries indicate Luau multi-return.
public record RawMethod(
    String luaName,
    String javaName,
    String description,
    List<RawGeneric> generics,
    List<RawParam> params,
    List<String> returns
) {}
