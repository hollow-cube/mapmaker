package net.hollowcube.luau.slopgen.docs;

/// A single `@luaReturn typeExpr [- description]` entry. The type expression is preserved as
/// the raw rest-of-line minus the optional `- description` suffix.
public record TagReturn(String typeExpr, String description) {}
