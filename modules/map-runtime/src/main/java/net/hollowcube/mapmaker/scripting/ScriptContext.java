package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.gen.runtime.GeneratedStringAtoms;
import net.hollowcube.luau.require.RequireResolver;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.scripting.api.LibBase$luau;
import net.hollowcube.mapmaker.scripting.api.LibPlayer$luau;
import net.hollowcube.mapmaker.scripting.api.LibPlayers$luau;
import net.hollowcube.mapmaker.scripting.api.LuaGlobals;
import net.hollowcube.mapmaker.scripting.require.AbstractModuleLoader;
import net.hollowcube.mapmaker.scripting.require.BundleModuleLoader;
import net.hollowcube.mapmaker.scripting.require.DynamicModuleLoader;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/// Script runner entrypoint. Create one per *editing* world (or once per
/// published world).
///
/// Two layers, with independent lifetimes:
///
///  - **Source layer** (this object's whole lifetime): the in-memory file store
///    plus the machinery to keep it fresh - the initial [#bootstrap] fetch and
///    every subsequent [#notifyFilesChanged]. For an editor world this runs from
///    the moment the world opens, *whether or not* a test world exists yet, so
///    files are always current and a test can start instantly.
///
///  - **Runtime layer** ([#attach] .. [#detach]): the actual Luau VM - the
///    [LuaState], [ScriptOwner]/scopes and the running entry script. This is
///    only created when there is a world to run against (the test world), and is
///    torn down when that world goes away. No Lua is created or run while
///    detached.
///
/// Reload safety: file changes are fetched off the world thread, but every
/// mutation of Lua state (cache clear, scope disposal, re-running the entry)
/// happens on the runtime world's scheduler thread, debounced, and only after
/// the new sources compile cleanly. A broken edit is reported and the previously
/// running scripts keep going - it never throws out of a background thread or
/// leaves a half-initialized world.
public final class ScriptContext {
    private static final Logger logger = LoggerFactory.getLogger(ScriptContext.class);

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    /// The entry script. Run manually (not via require) on its own scope.
    private static final String ENTRY_PATH = "/world.luau";
    private static final String ENTRY_CHUNK = "/world";

    private static final long DEBOUNCE_MILLIS = 150;

    /// Published maps: an immutable bundle that is always running. There is no
    /// "test world" concept here, so the runtime is attached immediately.
    public static ScriptContext bundled(MapWorld world, URI bundle) {
        try {
            var ctx = new ScriptContext(world.map().id(),
                new BundleModuleLoader(LUAU_COMPILER, bundle), null);
            ctx.attach(world);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("failed to open script bundle for " + world.map().id(), e);
        }
    }

    /// Editing flow: sources fetched from the backend and hot reloaded. The
    /// runtime is *not* started here - call [#bootstrap] on start, then
    /// [#attach]/[#detach] around the test world's lifetime.
    public static ScriptContext reloading(MapClient maps, String mapId) {
        var loader = new DynamicModuleLoader(LUAU_COMPILER, maps, mapId);
        return new ScriptContext(mapId, loader, loader);
    }

    private final String mapId;
    private final AbstractModuleLoader loader;
    /// Non-null only in reloading mode; same instance as {@link #loader}.
    private final @Nullable DynamicModuleLoader dynamic;

    /// The live Luau runtime, present only between [#attach] and [#detach].
    /// Volatile: attach/detach and notifyFilesChanged run off the world thread,
    /// while the reload drain runs on it.
    private volatile @Nullable Runtime runtime;

    private ScriptContext(String mapId, AbstractModuleLoader loader, @Nullable DynamicModuleLoader dynamic) {
        this.mapId = mapId;
        this.loader = loader;
        this.dynamic = dynamic;
    }

    //region Source layer (whole lifetime)

    /// Fetch every file for the map into the in-memory store. Blocking; call from
    /// a virtual/background thread on editor-world start. Safe to call with no
    /// runtime attached - it only warms the source store.
    @Blocking
    public void bootstrap() {
        if (dynamic == null) return; // bundled: already in memory
        try {
            dynamic.loadAllFiles();
        } catch (Exception e) {
            report("failed initial script fetch", e);
        }
    }

    /// A change source (NATS) calls this OFF the world thread when files change.
    /// The store is always refreshed so it stays current even with no test
    /// world; if a runtime is attached we additionally request a debounced,
    /// thread-confined reload of the live VM.
    @Blocking
    public void notifyFilesChanged(Collection<String> changedPaths) {
        if (dynamic == null) {
            logger.warn("[scripts:{}] ignoring file change in bundled (immutable) mode", mapId);
            return;
        }

        Set<String> changedChunks;
        try {
            changedChunks = dynamic.reloadFiles(changedPaths); // blocking IO, off-thread
        } catch (Exception e) {
            report("failed to fetch changed files", e);
            return;
        }

        var r = runtime;
        if (r == null) {
            // No test world yet: store is fresh, nothing to reload. When a test
            // world attaches it runs the current sources from scratch.
            logger.info("[scripts:{}] {} file(s) updated (no runtime attached)", mapId, changedChunks.size());
            return;
        }
        requestReload(r, changedChunks);
    }

    //endregion

    //region Runtime layer (attach .. detach)

    /// Start the Luau runtime against {@code world} (the test world) and run the
    /// entry script from the current source store. Idempotent-guarded.
    public void attach(MapWorld world) {
        if (runtime != null) {
            logger.warn("[scripts:{}] attach called while already attached, ignoring", mapId);
            return;
        }

        var global = createGlobalThread(loader);
        var r = new Runtime(world, global, new ScriptOwner(world));
        // Fresh run rebuilds the dependency graph from scratch.
        if (dynamic != null) dynamic.clear();
        this.runtime = r;

        world.scheduler().scheduleNextTick(() -> runEntry(r));
        logger.info("[scripts:{}] runtime attached", mapId);
    }

    /// Tear down the Luau runtime. The source store + change source are
    /// untouched, so the world can be re-attached later with current files.
    public void detach() {
        var r = runtime;
        if (r == null) return;
        this.runtime = null;
        try {
            r.owner.disposeAll();
        } finally {
            r.global.close();
        }
        logger.info("[scripts:{}] runtime detached", mapId);
    }

    /// Run the entry script on a fresh scope. Best effort: a failure is reported
    /// and swallowed so the world stays alive. World scheduler thread only.
    private void runEntry(Runtime r) {
        if (runtime != r) return; // detached/replaced before we got scheduled
        try {
            r.owner.disposeScope(ENTRY_CHUNK); // tear down any previous generation
            var thread = r.global.newThread();
            thread.setThreadData(new ScriptFrame(r.owner, r.owner.scope(ENTRY_CHUNK)));
            r.global.pop(1); // remove thread from the main stack; the frame keeps it reachable
            thread.sandboxThread();

            var bytecode = loader.readEntry(ENTRY_PATH);
            thread.load(ENTRY_CHUNK, bytecode);
            thread.call(0, 0);
        } catch (Exception e) {
            report("failed to run " + ENTRY_PATH, e);
        }
    }

    //endregion

    //region Reload management

    private void requestReload(Runtime r, Set<String> changedChunks) {
        // Hop to the world thread to touch pipeline state + schedule the drain.
        r.world.scheduler().scheduleNextTick(() -> {
            if (runtime != r) return; // detached while in flight
            r.pendingChunks.addAll(changedChunks);
            if (r.drainScheduled) return;
            r.drainScheduled = true;
            r.world.scheduler().buildTask(() -> drainReload(r))
                .delay(TaskSchedule.millis(DEBOUNCE_MILLIS))
                .schedule();
        });
    }

    /// The single place Lua state is mutated for a reload. World thread only.
    private void drainReload(Runtime r) {
        if (runtime != r) return; // detached before the debounce elapsed
        r.drainScheduled = false;
        if (dynamic == null || r.pendingChunks.isEmpty()) {
            r.pendingChunks.clear();
            return;
        }
        var changed = Set.copyOf(r.pendingChunks);
        r.pendingChunks.clear();

        // 1. Compile-check everything FIRST. On failure, abort the swap entirely:
        //    the previously running scripts are untouched.
        try {
            dynamic.compileCheck();
        } catch (Exception e) {
            report("reload aborted, scripts unchanged (compile error)", e);
            return;
        }

        // 2. Transitive invalidation from the dependency graph.
        var invalidated = dynamic.invalidateChunks(new HashSet<>(changed));
        invalidated.add(ENTRY_CHUNK); // the entry is always re-run

        // 3. Apply atomically on this thread.
        try {
            for (var chunk : invalidated) r.owner.disposeScope(chunk);
            r.global.requireClearCache(); // clear all - simple + correct (recompiles untouched modules)
            dynamic.clear(); // rebuilt by the re-run below
            runEntry(r);
            logger.info("[scripts:{}] reloaded ({} chunk(s) invalidated)", mapId, invalidated.size());
        } catch (Exception e) {
            // Should be rare (compile already passed); keep the world alive regardless.
            report("reload runtime error", e);
        }
    }

    private void report(String message, Throwable t) {
        logger.warn("[scripts:{}] {}", mapId, message, t);
        // TODO: surface to players via action bar (GenericTempActionBarProvider)
        //  once the api module is rebuilt.
    }

    //endregion

    /// Tear down the runtime (if any) and release the source store / loader.
    /// The NATS change source is owned and closed separately by the caller.
    public void close() {
        detach();
        try {
            loader.close();
        } catch (Exception ignored) {
        }
    }

    private static LuaState createGlobalThread(RequireResolver requireResolver) {
        var state = LuaState.newState();
        state.callbacks().userThread(ScriptContext::propagateThreadData);
        state.openLibs();
        state.openRequire(requireResolver);

        LuaGlobals.register(state);

        GeneratedStringAtoms.register(state);
        LibBase$luau.register(state);
        LibPlayers$luau.register(state);
        LibPlayer$luau.register(state);


        // TODO: register the api libraries here once that module is rebuilt.

        state.sandbox();
        return state;
    }

    /// Coroutine / task threads inherit their parent's frame verbatim, so a
    /// callback registered by chunk C stays attributed to C even when it runs
    /// later. Top-level (entry / required-module) threads set their own frame
    /// explicitly and have no parent here, so they are intentionally skipped.
    private static void propagateThreadData(@Nullable LuaState parent, LuaState thread) {
        if (parent == null) return; // Destruction, dont care
        if (parent.getThreadData() instanceof ScriptFrame frame)
            thread.setThreadData(frame);
    }

    /// The live Luau VM bound to one world. Recreated on every [#attach].
    private static final class Runtime {
        private final MapWorld world;
        private final LuaState global;
        private final ScriptOwner owner;

        // Reload pipeline state. Only touched on the world scheduler thread.
        private final Set<String> pendingChunks = new HashSet<>();
        private boolean drainScheduled;

        private Runtime(MapWorld world, LuaState global, ScriptOwner owner) {
            this.world = world;
            this.global = global;
            this.owner = owner;
        }
    }
}
