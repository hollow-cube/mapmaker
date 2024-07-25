package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

public interface LuaTypeMirror {

    LuaTypeMirror LUA_STATE_MARKER = new LuaState();

    @NotNull String luaType();
    @NotNull TypeName javaType();

    void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter);

    void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index);

    @NotNull String createCheck(int index);

    record Nullable(@NotNull LuaTypeMirror parent) implements LuaTypeMirror {

        @Override
        public @NotNull String luaType() {
            return parent.luaType() + "?";
        }

        @Override
        public @NotNull TypeName javaType() {
            return parent.javaType();
        }

        @Override
        public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
            method.beginControlFlow("if ($L == null)", getter);
            method.addStatement("state.pushNil()");
            method.nextControlFlow("else");
            parent.insertPush(method, getter);
            method.endControlFlow();
        }

        @Override
        public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
            method.addStatement("final $T $L;", parent.javaType(), name);
            method.beginControlFlow("if (state.getTop() < $L || state.isNil($L))", index, index);
            method.addStatement("$L = null", name);
            method.nextControlFlow("else");
            parent.insertPop(method, name + "$tmp", index);
            method.addStatement("$L = $L", name, name + "$tmp");
            method.endControlFlow();
        }

        @Override
        public @NotNull String createCheck(int index) {
            return "state.isNil(" + index + ") || " + parent.createCheck(index);
        }
    }

    record BasicPrimitive(
            @NotNull String luaType,
            @NotNull TypeName javaType,
            @NotNull String pushMethod,
            @NotNull String checkMethod
    ) implements LuaTypeMirror {
        @Override
        public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
            method.addStatement("state.$N($L)", pushMethod, getter);
        }

        @Override
        public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
            method.addStatement("var $L = state.$N($L)", name, checkMethod, index);
        }

        @Override
        public @NotNull String createCheck(int index) {
            return "state.is" + checkMethod + "(" + index + ")";
        }
    }

    record LuaState() implements LuaTypeMirror {

        @Override
        public @NotNull String luaType() {
            throw new UnsupportedOperationException("unreachable");
        }

        @Override
        public @NotNull TypeName javaType() {
            throw new UnsupportedOperationException("unreachable");
        }

        @Override
        public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
            throw new UnsupportedOperationException("unreachable");
        }

        @Override
        public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
            throw new UnsupportedOperationException("unreachable");
        }

        @Override
        public @NotNull String createCheck(int index) {
            throw new UnsupportedOperationException("unreachable");
        }
    }
}
