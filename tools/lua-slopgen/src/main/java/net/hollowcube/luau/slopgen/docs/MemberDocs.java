package net.hollowcube.luau.slopgen.docs;

import java.util.List;

/// The full extracted contents of a documentation comment in slopgen-relevant form: free-text
/// description plus the recognized `@luaParam` / `@luaReturn` / `@luaGeneric` block tags. Type
/// expressions are kept as raw strings here; the docs module parses them later.
///
/// Used uniformly across libraries, exports, methods, and accessors. Container-level docs
/// (library, export) only carry meaningful values in `description`; the validator rejects
/// param/return/generic tags at those positions.
public record MemberDocs(
    String description,
    List<TagGeneric> generics,
    List<TagParam> params,
    List<String> returns,
    List<TagDiagnostic> diagnostics
) {
    public static MemberDocs empty() {
        return new MemberDocs("", List.of(), List.of(), List.of(), List.of());
    }
}
