package net.hollowcube.luau.slopgen;

public final class LuaNames {
    public static final String RUNTIME_PACKAGE_NAME = "net.hollowcube.luau.gen.runtime";

    public static final String INDEX_META_NAME = "__index";
    public static final String NEWINDEX_META_NAME = "__newindex";
    public static final String NAMECALL_META_NAME = "__namecall";

    private LuaNames() {
    }

    /// Convert a Java property accessor name (`getOnJoin`, `setBreed`, `getX`) to its Lua
    /// counterpart. Strips the `get`/`set` prefix and snake-cases the remainder.
    public static String toLuaProperty(String javaName) {
        if (javaName.startsWith("get") || javaName.startsWith("set"))
            return toSnakeCase(javaName.substring(3));
        return toSnakeCase(javaName);
    }

    /// Convert a Java method name (`spawnEntity`, `luaToString`, `bark_`) to its Lua counterpart.
    /// Trailing underscores (used to dodge Java keyword conflicts like `wait_`) are dropped.
    public static String toLuaMethod(String javaName) {
        var name = javaName;
        if (name.endsWith("_")) name = name.substring(0, name.length() - 1);
        return toSnakeCase(name);
    }

    /// Convert a camelCase / PascalCase / mixed identifier to snake_case.
    ///
    /// Handles consecutive-uppercase acronyms and letter/digit boundaries:
    ///
    ///   - `URL`            → `url`
    ///   - `parseHTMLString` → `parse_html_string`
    ///   - `OnJoin`         → `on_join`
    ///   - `lua_to_string`  → `lua_to_string` (already snake; preserved)
    ///   - `option2`        → `option_2`
    ///   - `v2Handler`      → `v_2_handler`
    ///
    /// Three passes:
    ///   1. `_` between lower/digit and a following uppercase (`onJoin` → `on_Join`).
    ///   2. `_` before the last uppercase in a run that's followed by a lowercase
    ///      (`HTMLString` → `HTML_String`), so acronyms stay one token.
    ///   3. `_` between a letter and an adjacent digit (either direction), so
    ///      `v2Handler` → `v_2_Handler`.
    public static String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return input
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
            .replaceAll("([A-Za-z])([0-9])", "$1_$2")
            .replaceAll("([0-9])([A-Za-z])", "$1_$2")
            .toLowerCase();
    }

}
