package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.timer.Scheduler;

/// ScriptContext exists to manage resources associated with a given thread (group)
///
/// For example, the world itself gets a ScriptContext as well as any player scripts.
/// Threads created within those contexts will inherit the same ScriptContext (possibly
/// via a Holder, for example on a task or coroutine thread).
public sealed interface ScriptContext extends ThreadData {

    static ScriptContext get(LuaState state) {
        final Object data = state.getThreadData();
        if (data instanceof ThreadData threadData)
            return threadData.scriptContext();
        throw new IllegalStateException("ScriptContext not set (was '" + data + "')");
    }

    non-sealed interface World extends ScriptContext {

        MapWorld world();

    }

    non-sealed interface Player extends ScriptContext {

        MapPlayer player();

    }

    Scheduler scheduler();

    @Override
    default ScriptContext scriptContext() {
        return this;
    }
}
