package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Lua-thread-local context for script api evaluation.
///
/// This should be the entrypoint for any api logic which needs to interact with the runtime.
///
/// Stored in the lua thread data with the following rules:
///  - The entry thread gets a frame for the entry chunk's scope.
///  - A {@code require()}'d module thread gets a *new* frame for that module's
///    own scope (so a reload of the module only tears down its resources).
///  - Coroutine / task threads inherit their parent's frame verbatim (see
///    [ScriptEngine]'s userThread callback), so a deferred callback registered
///    by chunk C is still attributed to C even when it runs much later.
public record ScriptContext(ScriptRuntime runtime, ScriptScope scope) {
    private static final Logger logger = LoggerFactory.getLogger(ScriptContext.class);

    public static @Nullable ScriptContext current(LuaState state) {
        return state.getThreadData() instanceof ScriptContext frame ? frame : null;
    }

    public static void track(LuaState state, Disposable disposable) {
        var frame = current(state);
        if (frame == null) {
            // Even though this is probably wrong, we should not leak it.
            logger.warn("No script frame for thread, cannot track resource, disposing immediately. This is probably a bug.");
            disposable.dispose();
            return;
        }
        frame.scope().register(disposable);
    }
}
