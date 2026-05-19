package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.ScriptScope;
import net.hollowcube.mapmaker.scripting.api.LibBase.EventSource.EventPusher;
import net.hollowcube.mapmaker.scripting.util.LuaTarget;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.Nullable;

/// Bridges a Minestom event to a [LuaTarget]. One object is *both* the listener
/// registered on the world event node *and* the [Disposable] tracked into the
/// [ScriptScope], so the trigger and the target share one lifetime and one
/// atomic [#dispose].
///
/// The invocation kind follows the target type: a [LuaCallback] is called
/// (`:listen`/`:once`), a [LuaCoroutine] is resumed (`:wait`). No mode flag.
///
/// Safety contract: every entry into [#run] gates on liveness *before*
/// touching the Lua stack, and [#dispose] detaches the trigger *first*. Both
/// run on the world scheduler thread.
public final class LuaEventListener implements EventListener<Event>, Disposable {
    private final Class<? extends Event> eventType;
    private final EventPusher<? extends Event> pusher;
    private final LuaTarget target;
    private final boolean singleShot;

    private @Nullable EventNode<InstanceEvent> node;
    private boolean disposed;

    public LuaEventListener(Class<? extends Event> eventType, EventPusher<? extends Event> pusher,
                            LuaTarget target, boolean singleShot) {
        this.eventType = eventType;
        this.pusher = pusher;
        this.target = target;
        this.singleShot = singleShot;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Event> eventType() {
        return (Class<Event>) eventType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result run(Event event) {
        // Pre-push liveness gate: a same-tick reload may have disposed us while
        // we are still in the dispatch snapshot. Must be before any stack push.
        if (disposed || !target.isAlive()) return Result.EXPIRED;

        int n = ((EventPusher<Event>) pusher).pushArgs(target.state(), event);
        if (n < 0) return Result.INVALID; // pusher ignored this event; nothing pushed

        switch (target) {
            case LuaCallback c -> c.call(n, 0);
            case LuaCoroutine co -> co.resume(n);
        }

        if (singleShot) {
            dispose();
            return Result.EXPIRED;
        }
        return Result.SUCCESS;
    }

    /// Add to the current frame's world event node and track into its scope.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register(LuaState state) {
        var frame = ScriptContext.current(state);
        if (frame == null) {
            // No owning world: cannot bind a listener. Don't leak the refs.
            target.dispose();
            return;
        }
        this.node = frame.owner().world().eventNode();
        node.addListener((EventListener) this);
        ScriptContext.track(state, this);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void dispose() {
        if (disposed) return;
        // (1) detach the trigger first - nothing fresh can reach us afterwards.
        if (node != null) node.removeListener((EventListener) this);
        // (2) mark disposed (the pre-push gate reads this).
        disposed = true;
        // (3) release the Lua refs.
        target.dispose();
    }
}
