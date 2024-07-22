package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LuaTypeRegistry {
    private final Map<TypeName, LuaTypeMirror> knownTypes = new HashMap<>();

    public void defineBasicPrimitive(@NotNull TypeName javaType, @NotNull String luaType, @NotNull String pushMethod, @NotNull String checkMethod) {
        knownTypes.put(javaType.withoutAnnotations(), new LuaTypeMirror.BasicPrimitive(luaType, pushMethod, checkMethod));
    }

    public void define(@NotNull TypeName javaType, @NotNull LuaTypeMirror type) {
        knownTypes.put(javaType.withoutAnnotations(), type);
    }

    public @Nullable LuaTypeMirror forTypeName(@NotNull TypeName typeName) {
        if (typeName instanceof ParameterizedTypeName ptn) {
            //todo handle generics correctly. but for now this basically just unwraps Pin
            return knownTypes.get(ptn.rawType.withoutAnnotations());
        }
        return knownTypes.get(typeName.withoutAnnotations());
    }

    // String getter) {
    //                    method.addStatement("state.pushBoolean($L)", getter);
    //                }
    //
    //                @Override
    //                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
    //                    method.addStatement("boolean $L = state.checkBooleanArg($L)", name, in

}
