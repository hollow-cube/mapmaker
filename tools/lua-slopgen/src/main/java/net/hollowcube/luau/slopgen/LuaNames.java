package net.hollowcube.luau.slopgen;

public final class LuaNames {
    public static final String RUNTIME_PACKAGE_NAME = "net.hollowcube.luau.gen.runtime";

    public static final String INDEX_META_NAME = "__index";
    public static final String NEWINDEX_META_NAME = "__newindex";
    public static final String NAMECALL_META_NAME = "__namecall";

    public static String toLuaProperty(String javaName) {
        if (javaName.startsWith("get") || javaName.startsWith("set"))
            return toLuaMethod(javaName.substring(3));
        return javaName;
    }

    public static String toLuaMethod(String javaName) {
        return javaName.substring(0, 1).toLowerCase() + javaName.substring(1);
    }

}
