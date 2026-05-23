package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;

public sealed interface LuaResumable extends Disposable permits LuaCallback, LuaContinuation {

    /// The state which owns this. May be suspended currently.
    LuaState state();

    boolean isDisposed();

    void resume(int nargs);

    default void resume() {
        resume(0);
    }

}
