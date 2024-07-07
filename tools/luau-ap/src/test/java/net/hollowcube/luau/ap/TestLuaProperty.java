package net.hollowcube.luau.ap;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.PinImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLuaProperty {

    @LuaObject
    static class MethodPropHolder {

        @LuaProperty
        public @NotNull String getName() {
            return "theName";
        }

    }

    @Test
    void propertyMethod() {
        var state = LuaState.newState();
        var pin = (PinImpl<MethodPropHolder>) Pin.value(new MethodPropHolder());
        try {
            MethodPropHolder$Wrapper.initMetatable(state);
            pin.push(state);
            state.setGlobal("holder");

            eval(state, "return holder.Name");
            assertEquals("theName", state.toString(-1));
        } finally {
            pin.close();
            state.close();
        }
    }

    @LuaObject
    static class FieldPropHolder {

        @LuaProperty
        public final String name = "theName";

    }

    @Test
    void propertyField() {
        var state = LuaState.newState();
        var pin = (PinImpl<FieldPropHolder>) Pin.value(new FieldPropHolder());
        try {
            FieldPropHolder$Wrapper.initMetatable(state);
            pin.push(state);
            state.setGlobal("holder");

            eval(state, "return holder.Name");
            assertEquals("theName", state.toString(-1));
        } finally {
            pin.close();
            state.close();
        }
    }

    private static void eval(@NotNull LuaState state, @NotNull String source) {
        state.load("test.luau", compileLuau(source));
        assertDoesNotThrow(() -> state.pcall(0, 1));
    }

    private static byte[] compileLuau(@NotNull String source) {
        return assertDoesNotThrow(() -> LuauCompiler.DEFAULT.compile(source));
    }
}
