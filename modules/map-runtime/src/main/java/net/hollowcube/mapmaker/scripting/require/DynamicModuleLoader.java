package net.hollowcube.mapmaker.scripting.require;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.api.maps.MapClient;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/// Module loader for the editing/testing flow. Sources live in memory and are
/// (re)fetched from the backend via [MapClient]; the loader also tracks a
/// dependency graph so a changed file can transitively invalidate its dependents.
///
/// Fetching is blocking IO and must happen OFF the world thread (the change
/// source's virtual thread). The actual reload swap is driven by [net.hollowcube.mapmaker.scripting.ScriptContext]
/// on the world thread.
public final class DynamicModuleLoader extends AbstractModuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(DynamicModuleLoader.class);

    private final LuauCompiler compiler;
    private final MapClient maps;
    private final String mapId;

    /// File path (eg. /lib/util.luau) -> source.
    private final Map<String, String> sources = new HashMap<>();

    /// module chunk (eg. /lib/util) -> chunks that require it.
    private final Map<String, Set<String>> dependents = new HashMap<>();

    public DynamicModuleLoader(LuauCompiler compiler, MapClient maps, String mapId) {
        this.compiler = compiler;
        this.maps = maps;
        this.mapId = mapId;
    }

    @Override
    protected boolean isFile(String path) {
        return sources.containsKey(normalizeKey(path));
    }

    @Override
    protected byte[] readAndParseFile(String loadName) throws LuauCompileException {
        var source = Objects.requireNonNull(sources.get(normalizeKey(loadName)), loadName);
        return compiler.compile(source);
    }

    @Override
    protected void onResolved(String requiredBy, String required) {
        dependents.computeIfAbsent(required, _ -> new HashSet<>()).add(requiredBy);
    }

    @Blocking
    public void loadAllFiles() {
        sources.clear();
        for (var header : maps.listMapFiles(mapId)) {
            // TODO: only load luau files
            loadFile(header.path());
        }
    }

    /// Re-fetch the given changed paths and return the set of *chunk* names
    /// (no extension) that were affected, for the caller to invalidate.
    @Blocking
    public Set<String> reloadFiles(Collection<String> changedPaths) {
        var changedChunks = new HashSet<String>();
        for (var path : changedPaths) {
            var key = normalizeKey(path);
            loadFile(path);
            changedChunks.add(chunkOf(key));
        }
        return changedChunks;
    }

    @Blocking
    private void loadFile(String path) {
        var key = normalizeKey(path);
        byte @Nullable [] content = maps.getMapFile(mapId, stripLeadingSlash(key));
        if (content == null) {
            sources.remove(key);
            logger.info("[scripts:{}] removed {}", mapId, key);
        } else {
            sources.put(key, new String(content, StandardCharsets.UTF_8));
            logger.info("[scripts:{}] fetched {} ({} bytes)", mapId, key, content.length);
        }
    }

    /// Compile every current source, discarding the bytecode. Throws on the first
    /// failure so the reload pipeline can abort the swap before mutating Lua state.
    public void compileCheck() throws LuauCompileException {
        for (var entry : sources.entrySet()) {
            // TODO: we should track this so we dont 2x compile everything on reload.
            try {
                compiler.compile(entry.getValue());
            } catch (LuauCompileException e) {
                logger.warn("[scripts:{}] compile failed for {}", mapId, entry.getKey());
                throw e;
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

    /// Drop the dependency graph so it is rebuilt cleanly by the next full run.
    /// Non-destructive *during* a reload - only called once a run succeeds.
    public void clear() {
        dependents.clear();
    }

    private static String normalizeKey(String path) {
        var p = path.replace('\\', '/');
        if (p.startsWith("@")) p = p.substring(1);
        if (!p.startsWith("/")) p = "/" + p;
        return p;
    }

    private static String stripLeadingSlash(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }

    private static String chunkOf(String key) {
        for (var suffix : new String[]{".luau", ".lua"}) {
            if (key.endsWith(suffix)) return key.substring(0, key.length() - suffix.length());
        }
        return key;
    }
}
