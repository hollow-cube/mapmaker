package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;

/// A Lua *thread* (coroutine) captured by Java and resumed later (task
/// spawn/defer/delay, and an event `:wait`).
///
/// Pins the thread for the handle's whole lifetime, plus any args captured at
/// schedule time that must survive GC until the deferred resume. Released by a
/// single idempotent [#dispose] (the only unref site).
public final class LuaContinuation implements LuaResumable {
    private static final int[] EMPTY = new int[0];

    /// Pin {@code thread}. {@code argRefs} were already reffed by the caller and
    /// must survive GC until the deferred resume; pass none for wait/no-arg.
    public static LuaContinuation of(LuaState thread, int... argRefs) {
        thread.pushThread(thread);
        int sr = thread.ref(-1); // pin the thread to be resumed
        thread.pop(1);
        return new LuaContinuation(thread, sr, argRefs.length == 0 ? EMPTY : argRefs);
    }

    private final LuaState state;
    private final int stateRef; // pins `state`
    private final int[] argRefs;
    private boolean disposed;

    private LuaContinuation(LuaState state, int stateRef, int[] argRefs) {
        this.state = state;
        this.stateRef = stateRef;
        this.argRefs = argRefs;
    }

    public LuaState state() {
        return state;
    }

    public void resume(int nargs) {
        state.resume(null, nargs);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        state.unref(stateRef);
        for (int r : argRefs) state.unref(r);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
