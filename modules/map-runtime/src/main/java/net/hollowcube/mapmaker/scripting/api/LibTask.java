package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@LuaLibrary(name = "@mapmaker/task")
public final class LibTask {

    @LuaMethod
    public static int spawn(LuaState state) {
        LuaState thread = toThread(state, 1);

        // Resume immediately on the new thread
        resume(state, thread, copyArgs(state, thread));
        return 1;
    }

    @LuaMethod
    public static int defer(LuaState state) {
        LuaState thread = toThread(state, 1);

        // Schedule one tick later
        scheduleLater(thread, 1, refArgs(state));
        return 1;
    }

    @LuaMethod
    public static int delay(LuaState state) {
        int ticks = state.optInteger(1, 0);
        if (ticks < 0) state.argError(1, "must be a non-negative");

        LuaState thread = toThread(state, 2);

        scheduleLater(thread, ticks, refArgs(state));
        return 1;
    }

    @LuaMethod
    public static int wait(LuaState state) {
        int ticks = state.optInteger(1, 0);
        if (ticks < 0) state.argError(1, "must be a non-negative");

        if (!state.isYieldable())
            throw state.error("thread is not in a yieldable state");

        state.pushThread(state);
        scheduleLater(state, ticks, new int[0]);
        state.pop(1); // remove thread

        return state.yield(0);
    }

    @LuaMethod
    public static int cancel(LuaState state) {
        throw new UnsupportedOperationException("todo");
//        state.checkType(1, LuaType.THREAD);
//        var thread = Objects.requireNonNull(state.toThread(1)); // checked above
//
//        var context = ScriptContext.get(thread);
//        var task = context.getTag(TaskRef.ACTIVE_TASK);
//        if (task == null || task.isDisposed()) {
//            state.pushBoolean(false);
//            return 1;
//        }
//
//        task.dispose();
//        state.pushBoolean(true);
//        return 1;
    }

    // Leaves the thread on the stack at -1
    private static LuaState toThread(LuaState state, int argIndex) {
        return switch (state.type(argIndex)) {
            case THREAD -> {
                state.pushValue(argIndex); // push thread back for return
                yield Objects.requireNonNull(state.toThread(argIndex)); // checked above
            }
            case FUNCTION -> {
                var newThread = state.newThread();
                state.xpush(newThread, argIndex); // push f to thread
                yield newThread;
            }
            // todo better error, also use argError
            default -> throw state.error();
        };
    }

    /// Thread is expected to be on the stack at -1, it will remain after the call.
    private static void scheduleLater(LuaState state, int ticks, int[] argRefs) {
        int stateRef = state.ref(-1);

        var context = ScriptContext.get(state);
        var task = new ScheduledTask(state, stateRef, argRefs);
        task.schedule(context, ticks);
        context.track(task);
    }

    private static void resume(@Nullable LuaState caller, LuaState thread, int nargs) {
        thread.resume(caller, nargs);
        // dont care about the status because one of a new options occur:
        // 1: OK -> exit, no need to do anything
        // 2: YIELD -> simply return the thread. One of two things happen
        //    A: the caller takes the thread and resumes it again later
        //    B: a task.wait happened, and we have already saved+scheduled the thread
        // 3: ERROR -> it has already been thrown :)
    }

    private static int copyArgs(LuaState from, LuaState to) {
        int nargs = from.top() - 2;
        for (int i = 0; i < nargs; i++) {
            from.xpush(to, i + 2);
        }
        return nargs;
    }

    private static int[] refArgs(LuaState state) {
        int[] argRefs = new int[state.top() - 2];
        for (int i = 0; i < argRefs.length; i++) {
            argRefs[i] = state.ref(i + 2);
        }
        return argRefs;
    }

    private static final class ScheduledTask implements Disposable {
        private final LuaState state;
        private final int stateRef;
        private final int[] argRefs;

        private @Nullable Task task;
        private boolean disposed;

        private ScheduledTask(LuaState state, int stateRef, int[] argRefs) {
            this.state = state;
            this.stateRef = stateRef;
            this.argRefs = argRefs;
        }

        public void schedule(ScriptContext context, int ticks) {
            if (task != null) throw new IllegalStateException("task already scheduled");
            this.task = context.runtime().scheduler()
                .scheduleTask(this::execute, TaskSchedule.tick(ticks));
        }

        @Override
        public void dispose() {
            if (disposed) return;
            disposed = true;

            if (task != null) task.cancel();

            state.unref(stateRef);
            for (int argRef : argRefs) {
                state.unref(argRef);
            }
        }

        private TaskSchedule execute() {
            state.getRef(stateRef);
            for (int argRef : argRefs) {
                state.getRef(argRef);
            }

            // Dispose right before resume now that all refs have been re-added to stack.
            // All tasks are single-step (currently dont have repeating), so clean up prior to execution.
            dispose();
            resume(null, state, argRefs.length);
            return TaskSchedule.stop();
        }
    }
}
