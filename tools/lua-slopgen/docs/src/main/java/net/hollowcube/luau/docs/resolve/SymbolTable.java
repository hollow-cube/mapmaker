package net.hollowcube.luau.docs.resolve;

import net.hollowcube.luau.gen.docs.RawExport;
import net.hollowcube.luau.gen.docs.RawLibrary;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// Catalogue of every `@LuaExport` known across the engine, keyed by `module:Name`. Built from
/// the aggregated raw-library JSONs before any individual library's type expressions are
/// parsed, so the resolver always has the full cross-module view.
public final class SymbolTable {

    private final Map<String, SymbolEntry> byKey;
    private final Map<String, SymbolEntry> byJavaType;
    private final Map<String, Set<String>> exportsByModule;

    private SymbolTable(Map<String, SymbolEntry> byKey,
                        Map<String, SymbolEntry> byJavaType,
                        Map<String, Set<String>> exportsByModule) {
        this.byKey = byKey;
        this.byJavaType = byJavaType;
        this.exportsByModule = exportsByModule;
    }

    public static SymbolTable build(List<RawLibrary> libraries) {
        var byKey = new LinkedHashMap<String, SymbolEntry>();
        var byJavaType = new LinkedHashMap<String, SymbolEntry>();
        var exportsByModule = new LinkedHashMap<String, Set<String>>();
        for (var lib : libraries) {
            var inModule = exportsByModule.computeIfAbsent(lib.module(), k -> new LinkedHashSet<>());
            for (RawExport ex : lib.exports()) {
                var entry = new SymbolEntry(lib.module(), ex.luaName(), ex.javaType());
                byKey.put(entry.key(), entry);
                byJavaType.put(ex.javaType(), entry);
                inModule.add(ex.luaName());
            }
        }
        return new SymbolTable(byKey, byJavaType, exportsByModule);
    }

    public @Nullable SymbolEntry lookupByKey(String moduleName, String luaName) {
        return byKey.get(moduleName + ":" + luaName);
    }

    public @Nullable SymbolEntry lookupByJavaType(String javaType) {
        return byJavaType.get(javaType);
    }

    /// Resolve a bare type name in the current library. Returns the matching entry or null.
    public @Nullable SymbolEntry lookupBareInModule(String moduleName, String luaName) {
        var siblings = exportsByModule.get(moduleName);
        if (siblings == null || !siblings.contains(luaName)) return null;
        return lookupByKey(moduleName, luaName);
    }

    public Collection<SymbolEntry> entries() {
        return byKey.values();
    }
}
