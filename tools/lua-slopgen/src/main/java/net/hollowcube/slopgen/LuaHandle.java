package net.hollowcube.slopgen;

import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.annotation.MetaType;
import org.jetbrains.annotations.Nullable;

public record LuaHandle(
        TypeName owningType, String methodName,
        boolean isLuaStatic, boolean isStatic,
        boolean isProperty,
        @Nullable MetaType metaType
) {
}
