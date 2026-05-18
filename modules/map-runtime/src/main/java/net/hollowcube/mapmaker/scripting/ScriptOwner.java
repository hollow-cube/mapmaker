package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.timer.Scheduler;

import java.util.HashMap;
import java.util.Map;

/// The lifetime root for one world's scripts.
///
/// Owns the {@code chunk -> }[ReloadScope] registry plus a single persistent
/// scope for resources that should explicitly survive hot reloads. There is one
/// [ScriptOwner] per [ScriptContext] (i.e. per world).
///
/// Not thread safe: created and mutated only on the world scheduler thread.
public final class ScriptOwner {
    /// Chunk name used for the persistent scope. Resources here survive reloads;
    /// they are only released on [#disposeAll()] (world teardown).
    public static final String PERSISTENT_CHUNK = "<persistent>";

    private final MapWorld world;
    private final Map<String, ReloadScope> scopes = new HashMap<>();
    private final ReloadScope persistent;

    ScriptOwner(MapWorld world) {
        this.world = world;
        this.persistent = new ReloadScope(this, PERSISTENT_CHUNK);
        this.scopes.put(PERSISTENT_CHUNK, persistent);
    }

    public MapWorld world() {
        return world;
    }

    public Scheduler scheduler() {
        return world.scheduler();
    }

    /// Get the scope for a chunk, creating a fresh one if none exists. A "fresh"
    /// scope is what a chunk gets each time it (re)runs.
    public ReloadScope scope(String chunk) {
        return scopes.computeIfAbsent(chunk, c -> new ReloadScope(this, c));
    }

    public ReloadScope persistentScope() {
        return persistent;
    }

    /// Dispose a single chunk's current generation and forget it, so the next run
    /// of that chunk starts from a clean scope. The persistent scope is never
    /// dropped here.
    public void disposeScope(String chunk) {
        if (PERSISTENT_CHUNK.equals(chunk)) return;
        var scope = scopes.remove(chunk);
        if (scope != null) scope.dispose();
    }

    /// Tear down everything (world close / player leave). Persistent scope included.
    public void disposeAll() {
        for (var scope : scopes.values()) scope.dispose();
        scopes.clear();
    }
}
