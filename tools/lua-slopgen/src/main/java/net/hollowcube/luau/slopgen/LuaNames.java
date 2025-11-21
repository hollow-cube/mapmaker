package net.hollowcube.luau.slopgen;

public final class LuaNames {
    public static final String RUNTIME_PACKAGE_NAME = "net.hollowcube.luau.gen.runtime";

    public static final String INDEX_META_NAME = "__index";
    public static final String INDEX_GLUE_NAME = "index$meta";
    public static final String NAMECALL_META_NAME = "__namecall";
    public static final String NAMECALL_GLUE_NAME = "namecall$meta";

    public static String toLuaProperty(String javaName) {
        return javaName.startsWith("get") ? toLuaMethod(javaName.substring(3)) : javaName;
    }

    public static String toLuaMethod(String javaName) {
        return javaName.substring(0, 1).toLowerCase() + javaName.substring(1);
    }

}
