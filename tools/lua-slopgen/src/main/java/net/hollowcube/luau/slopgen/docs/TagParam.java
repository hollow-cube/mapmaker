package net.hollowcube.luau.slopgen.docs;

import net.hollowcube.luau.slopgen.parse.LibraryModelBuilder;

/// A single `@luaParam name[?] typeExpr [- description]` entry. The type expression is preserved
/// as the raw rest-of-line minus the optional `- description` suffix. Parsing of the Luau type
/// happens later, in [LibraryModelBuilder].
public record TagParam(String name, boolean optional, String typeExpr, String description) {}
