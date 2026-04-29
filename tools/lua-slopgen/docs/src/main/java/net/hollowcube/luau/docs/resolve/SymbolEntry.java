package net.hollowcube.luau.docs.resolve;

/// One entry in the global symbol table: an `@LuaExport` named `luaName` declared in
/// `module`. `javaType` is the fully-qualified Java type name as emitted in the raw JSON,
/// used to translate `superExport` references back into module-qualified form.
public record SymbolEntry(String module, String luaName, String javaType) {

    /// The `module:Name` key used by [SymbolTable].
    public String key() {
        return module + ":" + luaName;
    }

    /// The `@module.Name` form used for cross-library references in user-written tags.
    public String qualifiedName() {
        return module + "." + luaName;
    }
}
