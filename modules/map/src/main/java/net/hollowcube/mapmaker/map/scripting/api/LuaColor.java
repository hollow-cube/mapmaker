package net.hollowcube.mapmaker.map.scripting.api;

import net.hollowcube.common.util.StringUtil;
import net.hollowcube.luau.LuaState;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchMethod;

public class LuaColor {
    public static final String NAME = "Color";

    public static void init(@NotNull LuaState state) {
        // Metatable for Color
        state.newMetaTable(NAME);
        state.pushCFunction(LuaColor::luaMul, "__mul");
        state.setField(-2, "__mul");
        state.pushCFunction(LuaColor::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        state.pushCFunction(LuaColor::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaColor::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);

        // Global constant for constructor & constants
        state.newTable();
        state.pushCFunction(LuaColor::newColor, "new");
        state.setField(-2, "new");
        for (var namedColorEntry : NamedTextColor.NAMES.keyToValue().entrySet()) {
            push(state, namedColorEntry.getValue());
            state.setField(-2, StringUtil.snakeToPascal(namedColorEntry.getKey()));
        }
        state.setReadOnly(-1, true);
        state.setGlobal(NAME);
    }

    public static void push(@NotNull LuaState state, @NotNull RGBLike color) {
        state.newUserData(color);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    /// Color inputs can be:
    /// * A hex string (e.g. "#FF0000")
    /// * A hex integer (e.g. 0xFF0000)
    /// * A color userdata
    public static @NotNull RGBLike checkArg(@NotNull LuaState state, int index) {
        if (state.isString(index)) {
            final var hexString = state.checkStringArg(index);
            final var color = TextColor.fromHexString(hexString);
            if (color == null) state.argError(index, "Invalid color hex string: " + hexString);
            return color;
        } else if (state.isNumber(index)) {
            final var hexInt = state.checkIntegerArg(index);
            return TextColor.color(hexInt);
        } else if (state.isUserData(index)) {
            return (RGBLike) state.checkUserDataArg(index, NAME);
        }

        state.argError(index, "Expected a color string, integer, or userdata");
        return null; // Unreachable
    }

    public static int newColor(@NotNull LuaState state) {
        push(state, checkArg(state, 1));
        return 1;
    }

    // Properties

    private static int getR(@NotNull LuaState state, @NotNull RGBLike color) {
        state.pushNumber(color.red() / 255.0);
        return 1;
    }

    private static int getG(@NotNull LuaState state, @NotNull RGBLike color) {
        state.pushNumber(color.green() / 255.0);
        return 1;
    }

    private static int getB(@NotNull LuaState state, @NotNull RGBLike color) {
        state.pushNumber(color.blue() / 255.0);
        return 1;
    }

    // Methods

    private static int lerp(@NotNull LuaState state, @NotNull RGBLike color) {
        final RGBLike otherColor = checkArg(state, 1);
        final double t = state.checkNumberArg(2);

        push(state, TextColor.lerp((float) t, color, otherColor));
        return 1;
    }

    // Metamethods

    private static int luaMul(@NotNull LuaState state) {
        final RGBLike color = checkArg(state, 1);
        final double factor = state.checkNumberArg(2);
        push(state, TextColor.color(
                (int) (color.red() * factor),
                (int) (color.green() * factor),
                (int) (color.blue() * factor)
        ));
        return 1;
    }

    private static int luaToString(@NotNull LuaState state) {
        RGBLike color = checkArg(state, 1);
        state.pushString(String.format("#%02X", color.red()) +
                String.format("%02X", color.green()) +
                String.format("%02X", color.blue()));
        return 1;
    }

    private static int luaIndex(@NotNull LuaState state) {
        final RGBLike color = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            case "R" -> getR(state, color);
            case "G" -> getG(state, color);
            case "B" -> getB(state, color);
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final RGBLike color = checkArg(state, 1);
        state.remove(1); // Remove the userdata from the stack
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            case "Lerp" -> lerp(state, color);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }

}
