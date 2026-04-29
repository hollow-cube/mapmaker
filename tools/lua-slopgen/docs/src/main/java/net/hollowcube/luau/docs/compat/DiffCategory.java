package net.hollowcube.luau.docs.compat;

/// Categorisation of one finding produced by [EngineApiDiff]. `BREAKING_*` entries fail the
/// build; `NON_BREAKING_*` entries are informational.
///
/// V1 uses strict structural type equality, so any param/return type change reports as
/// `BREAKING_*_CHANGED` regardless of variance. The labels are forward-looking — a future v2
/// will graduate widening returns and narrowing params into directional checks.
public enum DiffCategory {
    BREAKING_REMOVAL,
    BREAKING_PARAM_CHANGED,
    BREAKING_RETURN_CHANGED,
    BREAKING_PARAM_REQUIRED,
    BREAKING_PARAM_ADDED_REQUIRED,
    BREAKING_GENERIC_REMOVED,
    BREAKING_SCOPE_CHANGE,
    BREAKING_SUPER_CHANGED,
    NON_BREAKING_ADDITION;

    public boolean isBreaking() {
        return name().startsWith("BREAKING_");
    }
}
