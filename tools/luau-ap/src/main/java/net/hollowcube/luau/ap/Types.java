package net.hollowcube.luau.ap;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

final class Types {
    public static final TypeName STRING = TypeName.get(String.class);

    public static final TypeName LUA_STATE = ClassName.get("net.hollowcube.luau", "LuaState");
    public static final TypeName PIN = ClassName.get("net.hollowcube.luau.util", "Pin");
    public static final TypeName PIN_IMPL = ClassName.get("net.hollowcube.luau.util", "PinImpl");

    public static final TypeName MINESTOM_POINT = ClassName.get("net.minestom.server.coordinate", "Point");
    public static final TypeName MINESTOM_VEC = ClassName.get("net.minestom.server.coordinate", "Vec");
}
