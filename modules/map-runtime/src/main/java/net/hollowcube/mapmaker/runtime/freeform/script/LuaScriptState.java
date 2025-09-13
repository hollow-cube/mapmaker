package net.hollowcube.mapmaker.runtime.freeform.script;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;

public final class LuaScriptState {

    public static LuaScriptState from(LuaState state) {
        return switch (state.getThreadData()) {
            case LuaScriptState threadState -> threadState;
            case Holder holder -> holder.scriptState();
            case null -> throw new IllegalStateException("No thread data set for LuaState: " + state);
            default ->
                    throw new IllegalArgumentException("Invalid thread data type: " + state.getThreadData().getClass());
        };
    }

    public static LuaScriptState create(FreeformMapWorld world) {
        var thread = world.globalState().newThread();
        thread.sandboxThread(); // Create mutable user space
        int ref = world.globalState().ref(-1);

        var luaScriptState = new LuaScriptState(world, thread, ref);
        thread.setThreadData(luaScriptState);
        return luaScriptState;
    }

    public interface Holder {
        LuaScriptState scriptState();
    }

    private final FreeformMapWorld world;

    private final LuaState state;
    private final int stateRef; // A ref in the global state keeping the thread alive.

    private LuaScriptState(FreeformMapWorld world, LuaState state, int stateRef) {
        this.world = world;
        this.state = state;
        this.stateRef = stateRef;
    }

    public FreeformMapWorld world() {
        return world;
    }

    public LuaState state() {
        return this.state;
    }

    public void close() {
        this.world.globalState().unref(this.stateRef);
        System.out.println("CLOSING CHILD THREAD WITH STATUS: " + this.state.status());
    }
}
