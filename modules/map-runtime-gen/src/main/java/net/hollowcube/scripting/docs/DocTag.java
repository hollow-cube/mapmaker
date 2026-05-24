package net.hollowcube.scripting.docs;

import net.hollowcube.scripting.LibraryModelBuilder;

public sealed interface DocTag {

    /// Malformed tag marker, not a functional tag
    record Diagnostic(String message) implements DocTag {}

    /// A single `@luaGeneric name[...] [- description]` declaration. `pack` is true when the source
    /// had `T...` (a generic type pack), false for an ordinary scalar generic `T`.
    record Generic(String name, boolean pack, String description) implements DocTag {}

    /// A single `@luaParam name[?] typeExpr [- description]` entry. The type expression is preserved
    /// as the raw rest-of-line minus the optional `- description` suffix. Parsing of the Luau type
    /// happens later, in [LibraryModelBuilder].
    record Param(String name, boolean optional, String typeExpr, String description) implements DocTag {}

    /// A single `@luaReturn typeExpr [- description]` entry. The type expression is preserved as
    /// the raw rest-of-line minus the optional `- description` suffix.
    record Return(String typeExpr, String description) implements DocTag {}

}
