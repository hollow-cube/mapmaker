package net.hollowcube.scripting.types;

import java.util.Set;

/// Luau built-in primitive type names. These are always in scope: never qualified, never
/// `require`d, never emitted as a declaration. Shared by the cross-module resolver (to accept
/// bare references) and the declaration emitter (to render them bare without an import).
public final class LuauBuiltins {

    public static final Set<String> PRIMITIVES = Set.of(
        "nil", "boolean", "number", "string", "thread", "buffer", "vector",
        "any", "unknown", "never");

    private LuauBuiltins() {
    }
}
