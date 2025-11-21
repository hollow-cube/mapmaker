package net.hollowcube.luau.slopgen;

import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.annotation.old.MetaType;
import org.jetbrains.annotations.Nullable;

// Contains the relevant type info about a
public record LuaHandle(
    TypeName owningType, String methodName,
    boolean isLuaStatic, boolean isStatic,
    boolean isProperty,
    @Nullable MetaType metaType
) {
}
