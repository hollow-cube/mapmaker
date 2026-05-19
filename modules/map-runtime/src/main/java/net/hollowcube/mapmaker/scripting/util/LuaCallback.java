package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;

/// A captured lua function value.
///
/// Keeps the function and its owning thread alive until disposed.
public final class LuaCallback implements Disposable {

    public static LuaCallback of(LuaState state, int fnIdx) {
        int fn = state.ref(fnIdx);
        state.pushThread(state);
        int sr = state.ref(-1);
        state.pop(1); // pop thread
        return new LuaCallback(state, sr, fn);
    }

    private final LuaState state;
    private final int stateRef; // pins the thread of `state`
    private final int fnRef;

    private boolean disposed;

    private LuaCallback(LuaState state, int stateRef, int fnRef) {
        this.state = state;
        this.stateRef = stateRef;
        this.fnRef = fnRef;
    }

    public LuaState state() {
        return state;
    }

    /// The caller is expected to push {@code nargs} args on [#state] prior to call.
    public void call(int nargs, int nret) {
        state.getRef(fnRef);
        state.insert(state.top() - nargs); // move fn beneath the nargs args
        state.call(nargs, nret);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        state.unref(fnRef);
        state.unref(stateRef);
    }

    public boolean isDisposed() {
        return disposed;
    }
}
