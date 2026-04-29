package net.hollowcube.luau.gen.docs;

/// JSON form of a `@LuaProperty` getter. `returnTypeExpr` carries the raw `@luaReturn` value.
public record RawGetter(String javaName, String description, String returnTypeExpr) {}
