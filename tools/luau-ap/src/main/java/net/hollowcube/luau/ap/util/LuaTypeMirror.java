package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.MethodSpec;
import org.jetbrains.annotations.NotNull;

public interface LuaTypeMirror {

    @NotNull String name();

    void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter);

    void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index);

    record BasicPrimitive(@NotNull String name, @NotNull String pushMethod,
                          @NotNull String checkMethod) implements LuaTypeMirror {
        @Override
        public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
            method.addStatement("state.$N($L)", pushMethod, getter);
        }

        @Override
        public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
            method.addStatement("boolean $L = state.$N($L)", name, checkMethod, index);
        }
    }
}
