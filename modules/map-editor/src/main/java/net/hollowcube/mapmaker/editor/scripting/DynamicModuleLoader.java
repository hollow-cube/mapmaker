package net.hollowcube.mapmaker.editor.scripting;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.scripting.require.AbstractModuleLoader;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/// Module loader for the editing/testing flow. Sources live in memory and are
/// (re)fetched from a [ScriptSource] (production: the backend over HTTP;
/// tests: an in-memory store); the loader also tracks a dependency graph so
/// a changed file can transitively invalidate its dependents.
///
/// Fetching is blocking IO and must happen OFF the world thread (the change
/// source's virtual thread). The actual reload swap is driven by
/// [ReloadingScriptSession] on the world thread.
public final class DynamicModuleLoader extends AbstractModuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(DynamicModuleLoader.class);

    private final LuauCompiler compiler;
    private final ScriptSource source;
    private final String label;

    /// File path (eg. /lib/util.luau) -> source.
    private final Map<String, String> sources = new HashMap<>();

    /// module chunk (eg. /lib/util) -> chunks that require it.
    private final Map<String, Set<String>> dependents = new HashMap<>();

    public DynamicModuleLoader(LuauCompiler compiler, ScriptSource source, String label) {
        this.compiler = compiler;
        this.source = source;
        this.label = label;
    }

    @Override
    protected boolean isFile(String path) {
        return sources.containsKey(normalizeKey(path));
    }

    @Override
    protected byte[] readAndParseFile(String loadName) {
        var src = Objects.requireNonNull(sources.get(normalizeKey(loadName)), loadName);
        try {
            return compiler.compile(src);
        } catch (LuauCompileException e) {
            throw new RuntimeException(loadName, e);
        }
    }

    @Override
    protected void onResolved(String requiredBy, String required) {
        dependents.computeIfAbsent(required, _ -> new HashSet<>()).add(requiredBy);
    }

    @Blocking
    public void loadAllFiles() {
        sources.clear();
        for (var path : source.listFiles()) {
            // TODO: only load luau files
            loadFile(path);
        }
    }

    /// Re-fetch the given changed paths and return the set of canonical chunk
    /// keys that were affected, for the caller to invalidate. Uses the *same*
    /// [AbstractModuleLoader#canonicalChunk] the resolver keys the graph,
    /// scopes and require cache on, so the sets line up exactly.
    @Blocking
    public Set<String> reloadFiles(Collection<String> changedPaths) {
        var changedChunks = new HashSet<String>();
        for (var path : changedPaths) {
            loadFile(path);
            changedChunks.add(AbstractModuleLoader.canonicalChunk(path));
        }
        return changedChunks;
    }

    @Blocking
    private void loadFile(String path) {
        var key = normalizeKey(path);
        byte @Nullable [] content = source.readFile(key);
        if (content == null) {
            sources.remove(key);
            logger.info("[scripts:{}] removed {}", label, key);
        } else {
            sources.put(key, new String(content, StandardCharsets.UTF_8));
            logger.info("[scripts:{}] fetched {} ({} bytes)", label, key, content.length);
        }
    }

    /// Compile every current source, discarding the bytecode. Throws on the first
    /// failure so the reload pipeline can abort the swap before mutating Lua state.
    public void compileCheck() {
        for (var entry : sources.entrySet()) {
            // TODO: we should track this so we dont 2x compile everything on reload.
            try {
                compiler.compile(entry.getValue());
            } catch (LuauCompileException e) {
                logger.warn("[scripts:{}] compile failed for {}", label, entry.getKey());
                throw new RuntimeException(entry.getKey(), e);
            }
        }
    }

    /// Given a set of changed chunk names, read all transitive
    /// dependents from the graph so they can be invalidated also.
    public Set<String> invalidateChunks(Set<String> changedChunks) {
        var out = new HashSet<String>();
        var queue = new ArrayDeque<>(changedChunks);
        while (!queue.isEmpty()) {
            var chunk = queue.poll();
            if (!out.add(chunk)) continue;

            var deps = dependents.get(chunk);
            if (deps != null) queue.addAll(deps);
        }
        return out;
    }

    /// Drop the whole dependency graph. Used only on a *fresh* attach: a new VM
    /// has an empty require cache, so the graph is rebuilt from scratch by the
    /// first run.
    public void clear() {
        dependents.clear();
    }

    /// Incremental graph maintenance for a *scoped* reload. Only the records
    /// keyed by an invalidated module are dropped: those modules are evicted
    /// from the require cache and re-run, so their incoming edges are rebuilt
    /// from scratch via [#onResolved] on the cache-miss reload. Records for
    /// modules that stay cached are left intact - a cached require never reaches
    /// the resolver, so they would never be rebuilt and dropping them would lose
    /// them permanently.
    ///
    /// Tradeoff: a stale `requiredBy` left by a since-removed `require` causes
    /// conservative *over*-invalidation on a later reload (safe - never stale
    /// code). Known gap: if an invalidated module adds a brand-new `require` of
    /// an *already-cached* module, that edge isn't recorded until the cached
    /// module is itself evicted - a later change to it could under-invalidate.
    /// Closing that needs edge recording at require-resolution time (every
    /// require, not just cache misses).
    public void invalidateGraph(Set<String> invalidated) {
        dependents.keySet().removeAll(invalidated);
    }

    private static String normalizeKey(String path) {
        var p = path.replace('\\', '/');
        if (p.startsWith("@")) p = p.substring(1);
        if (!p.startsWith("/")) p = "/" + p;
        return p;
    }
}
