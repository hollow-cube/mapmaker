package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.runtime.GeneratedStringAtoms;
import net.hollowcube.luau.require.RequireResolver;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.scripting.api.*;
import net.hollowcube.mapmaker.scripting.require.AbstractModuleLoader;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Scripting entrypoint bound to a single world (and that instance thread).
/// Not thread safe.
///
/// Wraps the luau vm and the script runtime.
/// Only works with precompiled bytecode, not responsible for editor behavior.
public final class ScriptEngine {
    private static final Logger logger = LoggerFactory.getLogger(ScriptEngine.class);

    private static final String ENTRY_PATH = "/main.luau";
    private static final String ENTRY_CHUNK = "/main";

    private final MapWorld world;
    private final LuaState global;
    private final AbstractModuleLoader loader;

    private final ScriptRuntime runtime;
    private volatile boolean closed;

    public ScriptEngine(MapWorld world, AbstractModuleLoader loader) {
        this.world = world;
        this.loader = loader;
        this.global = createGlobalState(loader);

        this.runtime = new ScriptRuntime(world);
        this.closed = false;

        world.scheduler().scheduleNextTick(this::runEntry);
        logger.info("[scripts:{}] script engine started", world.map().id());
    }

    public MapWorld world() {
        return world;
    }

    public Scheduler scheduler() {
        return world.scheduler();
    }

    public void runEntry() {
        if (closed) return; // sanity

        try {
            runtime.disposeScope(ENTRY_CHUNK); // tear down any previous generation

            var thread = global.newThread();
            int threadRef = global.ref(-1); // pin the thread while we execute it
            global.pop(1);
            try {
                thread.setThreadData(new ScriptContext(runtime, runtime.scope(ENTRY_CHUNK)));
                thread.sandboxThread();

                var bytecode = loader.readEntry(ENTRY_PATH);
                thread.load(ENTRY_CHUNK, bytecode);
                thread.call(0, 0);
            } finally {
                global.unref(threadRef);
            }
        } catch (Exception e) {
            report("failed to run " + ENTRY_PATH, e);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if (closed) return;
        closed = true;

        try {
            runtime.disposeAll();
        } finally {
            try {
                global.close();
            } finally {
                try {
                    loader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void disposeScope(String chunkName) {
        runtime.disposeScope(chunkName);
    }

    public void clearRequireCache(String chunkName) {
        global.requireClearCacheEntry(chunkName);
    }

    private void report(String message, Throwable t) {
        logger.warn("[scripts:{}] {}", world.map().id(), message, t);
        // TODO: surface to players somehow.
    }

    private static LuaState createGlobalState(RequireResolver requireResolver) {
        var state = LuaState.newState();
        state.callbacks().userThread(ScriptEngine::propagateThreadData);
        state.openLibs();
        state.openRequire(requireResolver);

        GeneratedStringAtoms.register(state);
        LuaGlobals.register(state);
        LuaVector.register(state);
        LuaRuntime$luau.register(state);
        LuaText$luau.register(state);

        LibTask$luau.register(state);

        LibBase$luau.register(state);

        LibItem$luau.register(state);
        LibItem.registerSlotGlobal(state);

        LibEntity$luau.register(state);

        LibStore$luau.register(state);

        LibPlayers$luau.register(state);
        LibPlayer$luau.register(state);

        // TODO: register the remaining api libraries here once that module is rebuilt.

        state.sandbox();
        return state;
    }

    /// Coroutine / task threads inherit their parent's context verbatim, so a
    /// callback registered by chunk C stays attributed to C even when it runs
    /// later.
    /// Top-level (entry / required-module) threads set their own frame
    /// explicitly and have no parent here.
    private static void propagateThreadData(@Nullable LuaState parent, LuaState thread) {
        if (parent == null) return; // Destruction, dont care
        if (parent.getThreadData() instanceof ScriptContext frame)
            thread.setThreadData(frame);
    }
}
