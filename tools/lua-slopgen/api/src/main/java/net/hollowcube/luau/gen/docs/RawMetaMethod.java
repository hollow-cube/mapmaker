package net.hollowcube.luau.gen.docs;

import java.util.List;

/// JSON form of a meta `@LuaMethod` (one declared with `meta != Meta.NONE`). `meta` is the
/// `Meta` enum constant name.
public record RawMetaMethod(
    String meta,
    String javaName,
    boolean isVoid,
    String description,
    List<RawGeneric> generics,
    List<RawParam> params,
    List<String> returns
) {}
