package net.hollowcube.mapmaker.map.scripting.api;

import net.hollowcube.luau.LuaState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.*;

public class LuaText {
    public static final String NAME = "Text";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Text
        state.newMetaTable(NAME);
        state.pushCFunction(LuaText::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        state.pushCFunction(LuaText::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaText::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(@NotNull LuaState state, @NotNull Component text) {
        state.newUserData(text);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    // Text can be one of the following:
    // - A string (unparsed)
    // - A table representing a component (e.g. { text = "Hello", color = "red" })
    // - A component userdata
    public static @NotNull Component checkArg(@NotNull LuaState state, int index) {
        if (state.isString(index)) {
            // If it's a string, we create a simple text component
            String text = state.checkStringArg(index);
            return Component.text(text);
        } else if (state.isTable(index)) {
            // If it's a table, we parse it into a component
            return parseComponentArg(state, index);
        } else if (state.isUserData(index)) {
            // If it's userdata, we assume it's already a Component
            return (Component) state.checkUserDataArg(index, NAME);
        }

        state.argError(index, "Expected a string, table, or Text");
        return null; // Unreachable
    }

    private static Component parseComponentArg(@NotNull LuaState state, int tableIndex) {
        // TODO: support more than just text and color
        var builder = Component.text();
        if (tableGet(state, tableIndex, "Text")) {
            builder.content(state.checkStringArg(-1));
            state.pop(1); // Remove text
        }
        if (tableGet(state, tableIndex, "Color")) {
            builder.color(TextColor.color(LuaColor.checkArg(state, -1)));
            state.pop(1); // Remove text
        }
        return builder.build();
    }

    // Properties

    // Methods

    // Metamethods

    private static int luaToString(@NotNull LuaState state) {
        var text = checkArg(state, 1);
        state.pushString(text.toString()); //todo not sure what serialization is most useful
        return 1;
    }

    private static int luaIndex(@NotNull LuaState state) {
        final Component text = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final Component text = checkArg(state, 1);
        state.remove(1); // Remove the userdata from the stack
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            default -> noSuchMethod(state, NAME, methodName);
        };
    }

}
