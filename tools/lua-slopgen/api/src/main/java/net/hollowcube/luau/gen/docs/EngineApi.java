package net.hollowcube.luau.gen.docs;

import java.util.Map;

/// Aggregated and resolved engine API: every `@LuaLibrary` known to the build, keyed by its
/// module name. Output of the `aggregateLuauApi` task and the lockfile shape.
///
/// Type expressions inside library bodies are still raw strings (the resolver verified they
/// parse and that all names resolve, but the canonical form is the user-written source).
public record EngineApi(
    int schemaVersion,
    String kind,
    Map<String, RawLibrary> libraries
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String KIND = "engine-api";
}
