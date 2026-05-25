package net.hollowcube.mapmaker.scripting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/// A disposal bag tied to one chunk's *current generation*.
///
/// Every side effect a script creates (event listeners, scheduled tasks, spawned
/// entities, ...) is registered into the scope of the chunk that created it. When
/// that chunk is reloaded or its ([ScriptRuntime]) is torn down, the scope is
/// disposed and everything it holds is cleaned up - in reverse registration order
/// so dependents tear down before the things they depend on.
///
/// Registrations return a [Tracked] handle. Callers whose resource has a natural
/// removal signal (a Minestom entity's `EntityDespawnEvent`, a one-shot timer firing,
/// etc.) should wire that signal to `Tracked.untrack()` so the scope's resource set
/// doesn't accumulate dead entries over the lifetime of a long-running chunk.
///
/// Not thread safe: all mutation is confined to the world scheduler thread (see
/// [ScriptEngine] and the editor reload pipeline), which is the only place
/// chunks are run, reloaded, or disposed.
public final class ScriptScope {
    private static final Logger logger = LoggerFactory.getLogger(ScriptScope.class);

    private final ScriptRuntime owner;
    private final String chunkName;
    /// Identity-ordered set — insertion order is preserved (for reverse-LIFO disposal) and
    /// per-entry removal via [Entry#untrack] is O(1). The previous `ArrayDeque` couldn't support
    /// O(1) untrack without an Object-equality search.
    private final LinkedHashSet<Entry> resources = new LinkedHashSet<>();
    private boolean disposed;

    ScriptScope(ScriptRuntime owner, String chunkName) {
        this.owner = owner;
        this.chunkName = chunkName;
    }

    public ScriptRuntime owner() {
        return owner;
    }

    public String chunkName() {
        return chunkName;
    }

    public boolean isDisposed() {
        return disposed;
    }

    /// Register a resource for cleanup. If the scope is already disposed the resource
    /// is disposed immediately, so a late registration (e.g. from an in-flight async
    /// callback after a reload) can never leak. Returns a [Tracked] handle the caller
    /// can use to detach from the scope ahead of disposal — see class doc.
    public Tracked register(Disposable disposable) {
        if (disposed) {
            disposable.dispose();
            return Tracked.NO_OP;
        }
        var entry = new Entry(this, disposable);
        resources.add(entry);
        return entry;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        // Snapshot before iterating — each `disposable.dispose()` may fire a callback (the
        // canonical case: an entity's `EntityDespawnEvent` listener) that calls `untrack()` on
        // a still-live entry, mutating `resources`. Snapshot decouples iteration from the live
        // set so the mutation is harmless.
        var snapshot = new ArrayList<>(resources);
        resources.clear();
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            var entry = snapshot.get(i);
            if (entry.untracked) continue;
            try {
                entry.disposable.dispose();
            } catch (Exception e) {
                logger.warn("Disposable failed during dispose of scope '{}'", chunkName, e);
            }
        }
    }

    private static final class Entry implements Tracked {
        final ScriptScope owner;
        final Disposable disposable;
        boolean untracked;

        Entry(ScriptScope owner, Disposable disposable) {
            this.owner = owner;
            this.disposable = disposable;
        }

        @Override
        public void untrack() {
            if (untracked) return;
            untracked = true;
            owner.resources.remove(this);
        }
    }
}
