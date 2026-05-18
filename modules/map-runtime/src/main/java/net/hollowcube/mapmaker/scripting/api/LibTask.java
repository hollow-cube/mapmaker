package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.LegacyScriptContext;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Schedule and manage threads. Run a function on a new thread, defer it to the next tick,
/// delay it by a number of ticks, pause the current thread, or cancel any of the above.
@LuaLibrary(name = "@mapmaker/task")
public final class LibTask {

    /// Runs `thread` immediately on a new thread. Any extra arguments are passed to it.
    /// Returns the thread so it can be cancelled later with `task.cancel`.
    ///
    /// ```luau
    /// task.spawn(function(name) print("hello", name) end, "world")
    /// ```
    ///
    /// @luaGeneric A...
    /// @luaGeneric R...
    /// @luaParam thread ((A...) -> R...) | thread - the function or thread to run
    /// @luaParam args A... - arguments passed to the function
    /// @luaReturn thread
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

    /// Runs `thread` on the next tick. Any extra arguments are passed to it.
    /// Returns the thread so it can be cancelled later.
    ///
    /// @luaGeneric A...
    /// @luaGeneric R...
    /// @luaParam thread ((A...) -> R...) | thread - the function or thread to run
    /// @luaParam args A... - arguments passed to the function
    /// @luaReturn thread
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

    /// Runs `thread` after `ticks` ticks. Any extra arguments are passed to it. Returns the
    /// thread so it can be cancelled later.
    ///
    /// @luaGeneric A...
    /// @luaGeneric R...
    /// @luaParam ticks number - the number of ticks to wait
    /// @luaParam thread ((A...) -> R...) | thread - the function or thread to run
    /// @luaParam args A... - arguments passed to the function
    /// @luaReturn thread
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

    /// Pauses the current thread for `ticks` ticks. Defaults to one tick.
    ///
    /// ```luau
    /// task.wait(20) -- pause for one second at 20 TPS
    /// ```
    ///
    /// @luaParam ticks number? - the number of ticks to wait
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

    /// Cancels a thread previously returned by `spawn`, `defer`, or `delay`. Returns `true`
    /// if the thread was running and is now cancelled, `false` if it had already finished.
    ///
    /// @luaParam thread thread
    /// @luaReturn boolean
    @LuaMethod
    public static int cancel(LuaState state) {
        state.checkType(1, LuaType.THREAD);
        var thread = Objects.requireNonNull(state.toThread(1)); // checked above

        var context = LegacyScriptContext.get(thread);
        var task = context.getTag(TaskRef.ACTIVE_TASK);
//        if (task == null || task.isDisposed()) {
//            state.pushBoolean(false);
//            return 1;
//        }

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
        var context = LegacyScriptContext.get(state);
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

//        @Override
//        public boolean isDisposed() {
//            return !task.isAlive();
//        }
    }
}
