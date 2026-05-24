package net.hollowcube.scripting.types;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/// Registry of [MetaType] expanders, keyed by name (including the `$` prefix).
/// New meta-types are added here as a one-line entry.
public final class MetaTypes {

    private static final Map<String, MetaType> KNOWN;

    static {
        var map = new LinkedHashMap<String, MetaType>();
        register(map, new WritableMetaType());
        KNOWN = Map.copyOf(map);
    }

    private static void register(Map<String, MetaType> map, MetaType mt) {
        map.put(mt.name(), mt);
    }

    private MetaTypes() {
    }

    /// True for any name starting with `$`. Used by the cross-module resolver to skip
    /// unresolved-name diagnostics on meta-type references (which are expanded later).
    public static boolean isMetaTypeName(String name) {
        return !name.isEmpty() && name.charAt(0) == '$';
    }

    /// Lookup by full name (e.g. `$Writable`). Returns null when the name is `$`-prefixed but
    /// not registered — callers should report this as an "unknown meta-type" error.
    public static @Nullable MetaType lookup(String name) {
        return KNOWN.get(name);
    }
}
