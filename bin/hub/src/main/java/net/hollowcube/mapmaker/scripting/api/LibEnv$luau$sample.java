package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;

import java.lang.foreign.Arena;

public final class LibEnv$luau$sample {
    private static final String LIB_NAME = "@mapmaker/env";

    private static final LuaFunc LIB_ENV_INDEX = LuaFunc.wrap(
        LibEnv$luau$sample::libEnv$index, "__index", Arena.global());

    private static final int PLAYER_USERDATA_TAG = 1;

    public static void pushPlayer(LuaState state, LibPlayer.Player player) {
        state.newUserDataTaggedWithMetatable(player, PLAYER_USERDATA_TAG);
    }

    public static LibPlayer.Player checkPlayerArg(LuaState state, int argIndex) {
        var result = state.toUserDataTagged(argIndex, PLAYER_USERDATA_TAG);
        if (result instanceof LibPlayer.Player actual) return actual;
        state.typeError(argIndex, "Player");
        return null; // Unreachable
    }

    public void register$luau(LuaState state) {
        // For the library object we dont actually care about the
        // userdata itself, none of the 'static' functions use it.
        state.newUserData(new Object());
        state.newTable(); // metatable

        state.pushFunction(LIB_ENV_INDEX);
        state.setField(-2, "__index");
        // other metamethods...

        state.setReadOnly(-1, true); // metatable read only
        state.setMetaTable(-2); // assign metatable ot userdata
        state.requireRegisterModule(LIB_NAME);


        {   // inner class player
            state.newTable(); // mt


            state.setReadOnly(-1, true); // mt read only
            state.setUserDataMetaTable(PLAYER_USERDATA_TAG); // set and pop mt
        }


    }

    private static int libEnv$index(LuaState state) {
        // index 1 is the userdata object, dont care.
        return switch (state.toStringAtomRaw(2)) {
            case 1/* Player */ -> LibEnv.getPlayer(state);
            case LuaState.NO_ATOM -> 0;
            default -> 0;
        };
    }

}
