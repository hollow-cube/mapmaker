package net.hollowcube.luau.gen.docs;

import java.util.List;

/// Top-level JSON document emitted per `@LuaLibrary` source class. One file lands in
/// `META-INF/luau-slopgen/<fqcn>.json` per library; the docs module aggregates them.
///
/// Type expressions on methods/accessors/params/returns are stored as raw strings here and
/// parsed downstream — keeping the AP free of any Luau parsing logic.
public record RawLibrary(
    int schemaVersion,
    String kind,
    String module,
    String scope,
    String sourceClass,
    String description,
    List<RawMethod> staticMethods,
    List<RawProperty> staticProperties,
    List<RawExport> exports
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String KIND = "raw-library";
}
