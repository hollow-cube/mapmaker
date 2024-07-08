package net.hollowcube.luau.ap;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.annotation.*;

public final class Types {
    public static final TypeName STRING = TypeName.get(String.class);

    // These have special treatment in MetaMethodType impl
    public static final TypeName MM_USER_TYPE = ClassName.get("unreachable", "UserType");
    public static final TypeName MM_ANY = ClassName.get("unreachable", "Any");

    public static final ClassName LUA_OBJECT = ClassName.get(LuaObject.class);
    public static final ClassName LUA_TYPE_IMPL = ClassName.get(LuaTypeImpl.class);

    public static final ClassName LUA_META = ClassName.get(LuaMeta.class);
    public static final ClassName LUA_PROPERTY = ClassName.get(LuaProperty.class);
    public static final ClassName LUA_METHOD = ClassName.get(LuaMethod.class);

    public static final TypeName LUA_STATE = ClassName.get("net.hollowcube.luau", "LuaState");
    public static final TypeName PIN = ClassName.get("net.hollowcube.luau.util", "Pin");
    public static final TypeName PIN_IMPL = ClassName.get("net.hollowcube.luau.util", "PinImpl");


}
