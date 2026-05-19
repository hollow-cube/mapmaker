package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;

/// A Lua *thread* (coroutine) captured by Java and resumed later (task
/// spawn/defer/delay, and an event `:wait`).
///
/// Pins the thread for the handle's whole lifetime, plus any args captured at
/// schedule time that must survive GC until the deferred resume. Released by a
/// single idempotent [#dispose] (the only unref site).
public final class LuaCoroutine implements Disposable {
    private static final int[] EMPTY = new int[0];

    /// Pin {@code thread}. {@code argRefs} were already reffed by the caller and
    /// must survive GC until the deferred resume; pass none for wait/no-arg.
    public static LuaCoroutine of(LuaState thread, int... argRefs) {
        thread.pushThread(thread);
        int sr = thread.ref(-1); // pin the thread to be resumed
        thread.pop(1);
        return new LuaCoroutine(thread, sr, argRefs.length == 0 ? EMPTY : argRefs);
    }

    private final LuaState state;
    private final int stateRef; // pins `state`
    private final int[] argRefs;
    private boolean disposed;

    private LuaCoroutine(LuaState state, int stateRef, int[] argRefs) {
        this.state = state;
        this.stateRef = stateRef;
        this.argRefs = argRefs;
    }

    public LuaState state() {
        return state;
    }

    /// The caller has already pushed {@code nargs} args on [#state] (e.g. an
    /// event payload for `:wait`); resume the thread.
    public void resume(int nargs) {
        state.resume(null, nargs);
    }

    /// Replay the saved deferred args onto the thread, then resume it
    /// (spawn/defer/delay, whose args were captured at schedule time).
    public void resume() {
        for (int r : argRefs) state.getRef(r);
        resume(argRefs.length);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        state.unref(stateRef);
        for (int r : argRefs) state.unref(r);
    }

    public boolean isDisposed() {
        return disposed;
    }
}
