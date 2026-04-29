package net.hollowcube.luau.gen.docs;

/// JSON representation of a `@luaParam` declaration. `typeExpr` is the raw, unparsed Luau
/// type expression as it appeared on the source line.
public record RawParam(String name, boolean optional, String typeExpr) {}
