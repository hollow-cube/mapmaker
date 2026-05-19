package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.ScriptRuntime;
import net.hollowcube.mapmaker.scripting.util.LuaTarget;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

/// Bridges a Minestom scheduler task to a [LuaTarget]. Covers the one-shot
/// case (spawn-later / defer / delay / a yielded `task.wait`) and the recurring
/// per-tick case (sidebar render).
///
/// One object owns the scheduled `Task`, the target, and (for one-shot tasks)
/// the [ScriptRuntime] cancel-registry binding, so the trigger and the target
/// share one lifetime and one atomic [#dispose]. Same safety contract as
/// [LuaEventListener]: the task body gates on liveness *before* any stack push,
/// and [#dispose] cancels the task *first*. World scheduler thread only.
public final class ScheduledCallback implements Disposable {
    private final LuaTarget target;
    private final ScriptRuntime owner;
    /// One-shot tasks register under their thread for `task.cancel`.
    private final boolean bindForCancel;

    private @Nullable Task task;
    private boolean disposed;

    private ScheduledCallback(LuaTarget target, ScriptRuntime owner, boolean bindForCancel) {
        this.target = target;
        this.owner = owner;
        this.bindForCancel = bindForCancel;
    }

    /// One-shot: resume {@code co} after {@code delayTicks} ticks. Registered
    /// under {@code co.state()} so `task.cancel(thread)` can find it.
    public static ScheduledCallback once(LuaState state, LuaCoroutine co, int delayTicks) {
        var owner = ScriptContext.current(state).owner();
        var sc = new ScheduledCallback(co, owner, true);
        sc.task = owner.scheduler().scheduleTask(() -> {
            if (sc.disposed || !co.isAlive()) return TaskSchedule.stop();
            co.resume();
            return TaskSchedule.stop();
        }, TaskSchedule.tick(delayTicks));
        owner.bindTask(co.state(), sc);
        ScriptContext.track(state, sc);
        return sc;
    }

    /// Recurring: run {@code tick} every tick (first run next tick) until
    /// disposed. {@code tick} owns its own stack discipline (push args, invoke
    /// the target, read results); this class only provides scheduling, the
    /// pre-run liveness gate, frame attribution and lifetime.
    public static ScheduledCallback recurring(LuaState state, LuaTarget target, Runnable tick) {
        var owner = ScriptContext.current(state).owner();
        var sc = new ScheduledCallback(target, owner, false);
        sc.task = owner.scheduler().scheduleTask(() -> {
            if (sc.disposed || !target.isAlive()) return TaskSchedule.stop();
            tick.run();
            return TaskSchedule.nextTick();
        }, TaskSchedule.nextTick());
        ScriptContext.track(state, sc);
        return sc;
    }

    public boolean isAlive() {
        return !disposed;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        // (1) detach the trigger first.
        if (task != null && task.isAlive()) task.cancel();
        // (2) mark disposed (the task body's pre-run gate reads this).
        disposed = true;
        if (bindForCancel) owner.unbindTask(target.state());
        // (3) release the Lua refs.
        target.dispose();
    }
}
