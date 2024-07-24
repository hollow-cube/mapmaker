package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.ap.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

public class LuaTypeRegistry {
    private final Map<TypeName, LuaTypeMirror> knownTypes = new HashMap<>();

    public void defineBasicPrimitive(@NotNull String luaType, @NotNull TypeName javaType, @NotNull String pushMethod, @NotNull String checkMethod) {
        knownTypes.put(javaType.withoutAnnotations(), new LuaTypeMirror.BasicPrimitive(luaType, javaType, pushMethod, checkMethod));
        if (javaType.isBoxedPrimitive()) {
            knownTypes.put(javaType.unbox(), new LuaTypeMirror.BasicPrimitive(luaType, javaType.unbox(), pushMethod, checkMethod));
        }
    }

    public void define(@NotNull TypeName javaType, @NotNull LuaTypeMirror type) {
        knownTypes.put(javaType.withoutAnnotations(), type);
    }

    public @Nullable LuaTypeMirror forTypeMirror(@NotNull TypeMirror typeMirror) {
        var typeName = TypeName.get(typeMirror).annotated(typeMirror.getAnnotationMirrors().stream().map(AnnotationSpec::get).toList());

        TypeName raw;
        if (typeName instanceof ParameterizedTypeName ptn)
            raw = ptn.rawType;
        else raw = typeName;
        if (raw.isBoxedPrimitive()) raw = raw.unbox();

        LuaTypeMirror type = knownTypes.get(raw.withoutAnnotations());

        // Try to make nullable if annotated
        if (type != null && raw.isAnnotated()) {
            for (var annotation : raw.annotations) {
                if (!annotation.type.equals(Types.JB_NULLABLE))
                    continue;
                type = new LuaTypeMirror.Nullable(type);
            }
        }

        return type;
    }

}
