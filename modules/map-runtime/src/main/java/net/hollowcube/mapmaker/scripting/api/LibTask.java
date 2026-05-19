package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.util.LuaCoroutine;
import net.hollowcube.mapmaker.scripting.util.ScheduledCallback;
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
        scheduleLater(thread, 1, refArgs(state));
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
        scheduleLater(thread, ticks, refArgs(state));
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

        scheduleLater(state, ticks, new int[0]);
        return state.yield(0);
    }

    /// Cancels a thread previously returned by `spawn`, `defer`, or `delay`. Returns `true`
    /// if the thread was still scheduled and is now cancelled, `false` if it had already
    /// finished or was never scheduled.
    ///
    /// @luaParam thread thread
    /// @luaReturn boolean
    @LuaMethod
    public static int cancel(LuaState state) {
        state.checkType(1, LuaType.THREAD);
        var thread = Objects.requireNonNull(state.toThread(1)); // checked above

        var frame = ScriptContext.current(thread);
        var sc = frame == null ? null : frame.owner().activeTask(thread);
        if (sc == null || !sc.isAlive()) {
            state.pushBoolean(false);
            return 1;
        }

        sc.dispose();
        state.pushBoolean(true);
        return 1;
    }

    /// Ref every extra arg (stack slots 2..top) so they survive until the
    /// deferred resume.
    private static int[] refArgs(LuaState state) {
        int[] argRefs = new int[state.top() - 2];
        for (int i = 0; i < argRefs.length; i++) {
            argRefs[i] = state.ref(i + 2);
        }
        return argRefs;
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

    /// Resume {@code thread} with {@code argRefs} after {@code ticks} ticks. The
    /// callback pins the thread + args and the scheduled-callback registers it
    /// under the thread so {@link #cancel} can find it.
    private static void scheduleLater(LuaState thread, int ticks, int[] argRefs) {
        var co = LuaCoroutine.of(thread, argRefs);
        ScheduledCallback.once(thread, co, ticks);
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
}
