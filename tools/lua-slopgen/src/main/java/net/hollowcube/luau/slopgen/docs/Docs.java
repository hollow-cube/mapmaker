package net.hollowcube.luau.slopgen.docs;

import net.hollowcube.luau.slopgen.LibraryModelBuilder;

import java.util.List;

/// The full extracted contents of a documentation comment in slopgen-relevant form: free-text
/// description plus the recognized `@luaParam` / `@luaReturn` / `@luaGeneric` block tags. Type
/// expressions are kept as raw strings here; [LibraryModelBuilder]
/// parses them into [net.hollowcube.luau.slopgen.types.LuauType] AST and stores the result on
/// the resolved [net.hollowcube.luau.slopgen.Model].
///
/// Used uniformly across libraries, exports, methods, and accessors. Container-level docs
/// (library, export) only carry meaningful values in `description`; the validator rejects
/// param/return/generic tags at those positions.
public record Docs(
    String description,
    List<DocTag.Generic> generics,
    List<DocTag.Param> params,
    List<DocTag.Return> returns,
    List<DocTag.Diagnostic> diagnostics
) {
    public static final Docs EMPTY = new Docs("", List.of(), List.of(), List.of(), List.of());

}
