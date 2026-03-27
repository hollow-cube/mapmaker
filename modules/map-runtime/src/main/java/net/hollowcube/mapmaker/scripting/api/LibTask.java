package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@LuaLibrary(name = "@mapmaker/task")
public final class LibTask {

    @LuaMethod
    public static int spawn(LuaState state) {
        LuaState thread = toThread(state, 1);

        // Copy the args to the thread
        int nargs = state.top() - 2;
        for (int i = 0; i < nargs; i++) {
            state.xpush(thread, i + 2);
        }

        // Resume the thread immediately
        resume(state, thread, nargs);
        return 1;
    }

    @LuaMethod
    public static int defer(LuaState state) {
        LuaState thread = toThread(state, 1);

        // Preserve the args for callback
        int[] argRefs = new int[state.top() - 2];
        for (int i = 0; i < argRefs.length; i++) {
            argRefs[i] = state.ref(i + 2);
        }

        // Schedule one tick later
        scheduleLater(thread, 1, argRefs);
        return 1;
    }

    @LuaMethod
    public static int delay(LuaState state) {
        int ticks = state.optInteger(1, 0);
        if (ticks < 0) state.argError(1, "must be a non-negative");

        LuaState thread = toThread(state, 2);

        // Preserve the args for callback
        int[] argRefs = new int[state.top() - 2];
        for (int i = 0; i < argRefs.length; i++) {
            argRefs[i] = state.ref(i + 2);
        }

        scheduleLater(thread, ticks, argRefs);
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
        state.checkType(1, LuaType.THREAD);
        var thread = Objects.requireNonNull(state.toThread(1)); // checked above

        var context = ScriptContext.get(thread);
        var task = context.getTag(TaskRef.ACTIVE_TASK);
        if (task == null || task.isDisposed()) {
            state.pushBoolean(false);
            return 1;
        }

        task.dispose();
        state.pushBoolean(true);
        return 1;
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
        int ref = state.ref(-1);
        var context = ScriptContext.get(state);
        var disposable = new TaskRef();
        disposable.state = state;
        disposable.threadRef = ref;
        disposable.argRefs = argRefs;
        disposable.task = context.scheduler().scheduleTask(() -> {
            state.getRef(ref);
            state.unref(ref);

            for (int argRef : argRefs) {
                state.getRef(argRef);
                state.unref(ref);
            }

            resume(null, state, argRefs.length);
            return TaskSchedule.stop();
        }, TaskSchedule.tick(ticks));
        context.track(disposable);
        context.setTag(TaskRef.ACTIVE_TASK, disposable);
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

    private static final class TaskRef implements Disposable {
        private static final Tag<TaskRef> ACTIVE_TASK = Tag.Transient("mapmaker/active_task");

        public LuaState state;
        public Task task;
        public int threadRef;
        public int[] argRefs;

        @Override
        public void dispose() {
            if (!task.isAlive()) return;

            task.cancel();
            state.unref(threadRef);
            for (int argRef : argRefs) {
                state.unref(argRef);
            }
        }

        @Override
        public boolean isDisposed() {
            return !task.isAlive();
        }
    }
}
