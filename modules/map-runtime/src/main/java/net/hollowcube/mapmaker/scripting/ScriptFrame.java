package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The object stored as Lua thread data for every script thread.
///
/// It pins down two things for whatever code is running on that thread:
///  - [#owner()] - which world's scripts these are (lifetime root), and
///  - [#scope()] - which chunk's [ReloadScope] new resources are attributed to.
///
/// Threading rules:
///  - The entry thread gets a frame for the entry chunk's scope.
///  - A {@code require()}'d module thread gets a *new* frame for that module's
///    own scope (so a reload of the module only tears down its resources).
///  - Coroutine / task threads inherit their parent's frame verbatim (see
///    [ScriptContext]'s userThread callback), so a deferred callback registered
///    by chunk C is still attributed to C even when it runs much later.
public record ScriptFrame(ScriptOwner owner, ReloadScope scope) {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFrame.class);

    public static @Nullable ScriptFrame current(LuaState state) {
        return state.getThreadData() instanceof ScriptFrame frame ? frame : null;
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
