package net.hollowcube.scripting.docs;

import net.hollowcube.scripting.LibraryModelBuilder;
import net.hollowcube.scripting.Model;
import net.hollowcube.scripting.types.LuauType;

import java.util.List;

/// The full extracted contents of a documentation comment in slopgen-relevant form: free-text
/// description plus the recognized `@luaParam` / `@luaReturn` / `@luaGeneric` block tags. Type
/// expressions are kept as raw strings here; [LibraryModelBuilder]
/// parses them into [LuauType] AST and stores the result on
/// the resolved [Model].
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
