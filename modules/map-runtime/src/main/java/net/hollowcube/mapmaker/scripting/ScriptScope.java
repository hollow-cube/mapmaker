package net.hollowcube.mapmaker.scripting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/// A disposal bag tied to one chunk's *current generation*.
///
/// Every side effect a script creates (event listeners, scheduled tasks, spawned
/// entities, ...) is registered into the scope of the chunk that created it. When
/// that chunk is reloaded or its ([ScriptRuntime]) is torn down, the scope is
/// disposed and everything it holds is cleaned up - in reverse registration order
/// so dependents tear down before the things they depend on.
///
/// Not thread safe: all mutation is confined to the world scheduler thread (see
/// [ScriptEngine] and the editor reload pipeline), which is the only place
/// chunks are run, reloaded, or disposed.
public final class ScriptScope {
    private static final Logger logger = LoggerFactory.getLogger(ScriptScope.class);

    private final ScriptRuntime owner;
    private final String chunkName;
    private final Deque<Disposable> resources = new ArrayDeque<>();
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
    /// callback after a reload) can never leak.
    public void register(Disposable disposable) {
        if (disposed) {
            disposable.dispose();
            return;
        }
        resources.push(disposable);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        Disposable disposable;
        while ((disposable = resources.poll()) != null) {
            try {
                disposable.dispose();
            } catch (Exception e) {
                logger.warn("Disposable failed during dispose of scope '{}'", chunkName, e);
            }
        }
    }
}
