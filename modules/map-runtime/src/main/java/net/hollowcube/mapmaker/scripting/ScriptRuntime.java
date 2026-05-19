package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.timer.Scheduler;

import java.util.HashMap;
import java.util.Map;

/// The lifetime root for one world's scripts.
///
/// Owns the {@code chunk -> }[ScriptScope] registry plus a single persistent
/// scope for resources that should explicitly survive hot reloads. There is one
/// [ScriptRuntime] per [ScriptEngine] (i.e. per world).
///
/// Not thread safe: created and mutated only on the world scheduler thread.
public final class ScriptRuntime {
    private final MapWorld world;
    private final Map<String, ScriptScope> scopes = new HashMap<>();

    ScriptRuntime(MapWorld world) {
        this.world = world;
    }

    public MapWorld world() {
        return world;
    }

    public Scheduler scheduler() {
        return world.scheduler();
    }

    public ScriptScope scope(String chunkName) {
        return scopes.computeIfAbsent(chunkName, c -> new ScriptScope(this, c));
    }

    public void disposeScope(String chunkName) {
        var scope = scopes.remove(chunkName);
        if (scope != null) scope.dispose();
    }

    public void disposeAll() {
        for (var scope : scopes.values()) scope.dispose();
        scopes.clear();
    }

}
