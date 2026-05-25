package net.hollowcube.mapmaker.scripting;

/// Handle returned by [ScriptContext#track] / [ScriptScope#register]. Lets the caller detach the
/// registration when the underlying resource has been (or is about to be) disposed by some other
/// path — typically wired to the resource's own removal/despawn event so the scope's track list
/// doesn't accumulate dead entries over a long-lived chunk.
///
/// `untrack()` does NOT invoke the original [Disposable]. The caller untracks *because* they know
/// the resource is already gone (or they're handing ownership elsewhere); the script context just
/// needs to drop its claim on the lifetime.
///
/// Idempotent. Single-threaded — must be called on the world scheduler thread, the same thread
/// that owns [ScriptScope] mutation.
public interface Tracked {

    void untrack();

    /// Sentinel for cases where there was nothing to track (resource already disposed at
    /// registration time, scope already disposed, etc.). Calling `untrack()` is a no-op.
    Tracked NO_OP = () -> {};
}
