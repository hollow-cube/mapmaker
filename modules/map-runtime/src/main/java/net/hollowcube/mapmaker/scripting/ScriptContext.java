package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.entity.EntityDespawnEvent;
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

    public static ScriptContext get(LuaState state) {
        var context = current(state);
        if (context == null) {
            throw new IllegalStateException("No script context for thread, cannot run API logic. This is probably a bug.");
        }
        return context;
    }

    public static Tracked track(LuaState state, Disposable disposable) {
        var context = current(state);
        if (context == null) {
            // Even though this is probably wrong, we should not leak it.
            logger.warn("No script frame for thread, cannot track resource, disposing immediately. This is probably a bug.");
            disposable.dispose();
            return Tracked.NO_OP;
        }
        return context.track(disposable);
    }

    /// Track a Minestom entity for the current script's lifetime. Wires
    /// [EntityDespawnEvent] on the entity's event node so that any external removal
    /// (game logic, world unload, manual `entity.remove()`) automatically detaches
    /// the registration — keeping the scope's resource set bounded over a long-lived
    /// chunk that spawns and discards many entities.
    ///
    /// Returns the same [Tracked] handle as [#track], in case the caller has another
    /// reason to detach (e.g. transferring ownership out of the script).
    public static Tracked trackEntity(LuaState state, Entity entity) {
        if (entity.isRemoved()) {
            // Already gone — nothing to clean up, no point hooking the despawn event.
            return Tracked.NO_OP;
        }
        var handle = track(state, entity::remove);
        entity.eventNode().addListener(EntityDespawnEvent.class, e -> handle.untrack());
        return handle;
    }

    public Tracked track(Disposable disposable) {
        return scope.register(disposable);
    }
}
