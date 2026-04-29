package net.hollowcube.luau.gen.docs;

/// JSON form of a `@LuaProperty` setter. `paramName` and `paramTypeExpr` come from the single
/// required `@luaParam` declaration on the setter.
public record RawSetter(
    String javaName,
    String description,
    String paramName,
    String paramTypeExpr
) {}
