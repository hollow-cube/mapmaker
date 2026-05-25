package net.hollowcube.scripting.types;

import net.hollowcube.scripting.Model;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// Catalogue of every `@LuaExport` known across the engine, keyed by `module:Name`. Built from
/// the aggregated [Model.Library] entries before any individual library's type expressions are
/// re-walked by the resolver, so the resolver always has the full cross-module view.
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

    public static SymbolTable build(Collection<Model.Library> libraries) {
        var byKey = new LinkedHashMap<String, SymbolEntry>();
        var byJavaType = new LinkedHashMap<String, SymbolEntry>();
        var exportsByModule = new LinkedHashMap<String, Set<String>>();
        for (var lib : libraries) {
            var inModule = exportsByModule.computeIfAbsent(lib.moduleName(), k -> new LinkedHashSet<>());
            for (Model.Export ex : lib.exports()) {
                var javaType = ex.javaType().toString();
                var entry = new SymbolEntry(lib.moduleName(), ex.luaName(), javaType);
                byKey.put(entry.key(), entry);
                byJavaType.put(javaType, entry);
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
