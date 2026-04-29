package net.hollowcube.luau.slopgen.docs;

/// A single `@luaParam name[?] typeExpr` entry, with the type expression preserved as the
/// raw rest-of-line — parsing of the Luau type happens later in the docs module.
public record TagParam(String name, boolean optional, String typeExpr) {}
