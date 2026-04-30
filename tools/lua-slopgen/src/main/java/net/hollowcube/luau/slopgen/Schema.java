package net.hollowcube.luau.slopgen;

import java.util.Map;

/// The canonical JSON document shape published by slopgen. A per-library fragment is a `Schema`
/// with one entry in `libraries`; the aggregate `engine-api.json` is a `Schema` with every
/// library known to the build. Both are consumed via the same gson adapter in `serde/SchemaJson`.
///
/// Type expressions inside library bodies are fully-resolved [types.LuauType] AST — string
/// parsing happens once in the annotation processor.
public record Schema(
    int schemaVersion,
    String kind,
    Map<String, Model.Library> libraries
) {
}
