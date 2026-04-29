package net.hollowcube.luau.gen.docs;

import java.util.List;

/// JSON form of a `@LuaExport`. `javaType` and `superExport` are fully-qualified Java type
/// names — the docs module resolver translates these into module-qualified Lua names like
/// `@mapmaker/players.Player` using the per-library `module` field as the keying primitive.
public record RawExport(
    String luaName,
    String javaType,
    String description,
    String superExport,
    boolean isFinal,
    List<RawMethod> methods,
    List<RawProperty> properties,
    List<RawMetaMethod> metaMethods
) {}
