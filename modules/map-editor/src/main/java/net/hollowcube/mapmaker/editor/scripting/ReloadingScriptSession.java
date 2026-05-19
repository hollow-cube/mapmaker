package net.hollowcube.mapmaker.editor.scripting;

import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/// The editing/testing half of the scripting system: it owns the source store
/// and the hot-reload pipeline, and drives a runtime-module [ScriptEngine]
/// through its few primitives.
///
/// Two layers with independent lifetimes:
///
///  - **Source layer** (this object's whole lifetime): the in-memory file store
///    plus the machinery to keep it fresh - the initial [#bootstrap] fetch and
///    every subsequent [#notifyFilesChanged]. Runs from the moment the editor
///    world opens, whether or not a test world exists yet.
///
///  - **Runtime layer** ([#attach] .. [#detach]/[#close]): a live
///    [ScriptEngine]. Only present when there is a test world to run against.
///
/// Reload safety: file changes are fetched off the world thread, but every
/// mutation of Lua state happens on the runtime world's scheduler thread,
/// debounced, and only after the new sources compile cleanly. A broken edit is
/// reported and the previously running scripts keep going.
public final class ReloadingScriptSession {
    private static final Logger logger = LoggerFactory.getLogger(ReloadingScriptSession.class);

    private static final long DEBOUNCE_MILLIS = 150;

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    /// Editing flow: sources fetched from the backend and hot reloaded. The
    /// runtime is *not* started here - call [#bootstrap] on start, then
    /// [#attach]/[#detach] around the test world's lifetime.
    public static ReloadingScriptSession reloading(MapClient maps, String mapId) {
        return new ReloadingScriptSession(mapId, new DynamicModuleLoader(LUAU_COMPILER, maps, mapId));
    }

    private final String mapId;
    private final DynamicModuleLoader dynamic;

    /// The live runtime, present only between [#attach] and [#detach]/[#close].
    /// Volatile: attach/detach and notifyFilesChanged run off the world thread,
    /// while the reload drain runs on it.
    private volatile @Nullable ScriptEngine runtime;

    // Reload pipeline state. Only touched on the world scheduler thread.
    private final Set<String> pendingChunks = new HashSet<>();
    private boolean drainScheduled;

    private ReloadingScriptSession(String mapId, DynamicModuleLoader dynamic) {
        this.mapId = mapId;
        this.dynamic = dynamic;
    }

    //region Source layer (whole lifetime)

    /// Fetch every file for the map into the in-memory store. Blocking; call from
    /// a virtual/background thread on editor-world start. Safe to call with no
    /// runtime attached - it only warms the source store.
    @Blocking
    public void bootstrap() {
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

    /// Start a runtime against {@code world} (the test world) and run the entry
    /// script from the current source store. Idempotent-guarded.
    public void attach(MapWorld world) {
        if (runtime != null) {
            logger.warn("[scripts:{}] attach called while already attached, ignoring", mapId);
            return;
        }

        // Fresh run rebuilds the dependency graph from scratch.
        dynamic.clear();
        pendingChunks.clear();
        drainScheduled = false;
        this.runtime = new ScriptEngine(world, dynamic, mapId);
    }

    /// Tear down the runtime. The source store + change source are untouched, so
    /// the world can be re-attached later with current files.
    public void detach() {
        var r = runtime;
        if (r == null) return;
        this.runtime = null;
        r.close();
        logger.info("[scripts:{}] runtime detached", mapId);
    }

    /// Tear down the runtime (if any). The NATS change source is owned and closed
    /// separately by the caller.
    public void close() {
        detach();
    }

    //endregion

    //region Reload management

    private void requestReload(ScriptEngine r, Set<String> changedChunks) {
        // Hop to the world thread to touch pipeline state + schedule the drain.
        r.scheduler().scheduleNextTick(() -> {
            if (runtime != r) return; // detached while in flight
            pendingChunks.addAll(changedChunks);
            if (drainScheduled) return;
            drainScheduled = true;
            r.scheduler().buildTask(() -> drainReload(r))
                .delay(TaskSchedule.millis(DEBOUNCE_MILLIS))
                .schedule();
        });
    }

    /// The single place Lua state is mutated for a reload. World thread only.
    private void drainReload(ScriptEngine r) {
        if (runtime != r) return; // detached before the debounce elapsed
        drainScheduled = false;
        if (pendingChunks.isEmpty()) return;
        var changed = Set.copyOf(pendingChunks);
        pendingChunks.clear();

        // 1. Compile-check everything FIRST. On failure, abort the swap entirely:
        //    the previously running scripts are untouched.
        try {
            dynamic.compileCheck();
        } catch (Exception e) {
            report("reload aborted, scripts unchanged (compile error)", e);
            return;
        }

        // 2. Transitive invalidation from the dependency graph. The entry chunk
        //    is always re-run by ScriptEngine#runEntry, so we only need to
        //    dispose the invalidated *modules* here.
        var invalidated = dynamic.invalidateChunks(new HashSet<>(changed));

        // 3. Apply atomically on this thread. Scoped: only the invalidated
        //    closure has its scope disposed and its require-cache entry evicted,
        //    so unchanged modules keep their resources *and* their cached value
        //    (their top-level body does not re-run). The graph is maintained
        //    incrementally to match.
        try {
            for (var chunk : invalidated) {
                r.disposeScope(chunk);
                r.clearRequireCache(chunk);
            }
            dynamic.invalidateGraph(invalidated);
            r.runEntry();
            logger.info("[scripts:{}] reloaded ({} chunk(s) invalidated)", mapId, invalidated.size());
        } catch (Exception e) {
            // Should be rare (compile already passed); keep the world alive regardless.
            report("reload runtime error", e);
        }
    }

    private void report(String message, Throwable t) {
        logger.warn("[scripts:{}] {}", mapId, message, t);
        // TODO: surface to players via action bar once the api module is rebuilt.
    }

    //endregion
}
