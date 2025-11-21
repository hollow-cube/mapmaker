package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@LuaLibrary(name = "@mapmaker/task")
public final class LibTask {

    @LuaMethod
    public static int spawn(LuaState state) {
        LuaState thread = switch (state.type(1)) {
            case THREAD -> {
                state.pushValue(1); // push thread back for return
                yield Objects.requireNonNull(state.toThread(1)); // checked above
            }
            case FUNCTION -> {
                var newThread = state.newThread();
                state.xpush(newThread, 1); // push f to thread
                yield newThread;
            }
            // todo better error, also use argError
            default -> throw state.error();
        };

        // Copy the args to the thread
        for (int i = 2; i < state.top(); i++) {
            state.xpush(thread, i);
        }

        // Resume the thread immediately
        return resume(state, thread);
    }

    @LuaMethod
    public static int defer(LuaState state) {

        // todo: schedule to run `ticks` later, blah blah
        //       then return the thread

        return 1;
    }

    @LuaMethod
    public static int delay(LuaState state) {
        int ticks = state.optInteger(1, 0);
        if (ticks < 0) state.argError(1, "must be a non-negative");

        // todo: schedule to run `ticks` later, blah blah
        //       then return the thread

        return 1;
    }

    @LuaMethod
    public static int wait(LuaState state) {
        int ticks = state.optInteger(1, 0);
        if (ticks < 0) state.argError(1, "must be a non-negative");

        if (!state.isYieldable())
            state.error("thread is not in a yieldable state");

        state.pushThread(state);
        int ref = state.ref(-1);
        state.pop(1); // remove thread

        // TODO: this doesnt really work because the entire state may be destroyed
        //       before this callback occurs. We need to store this in the script
        //       context until it executes so that we can cancel early.
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            state.getRef(1);
            state.unref(ref);

            resume(null, state);
            return TaskSchedule.stop();
        }, TaskSchedule.tick(ticks));

        return state.yield(0);
    }

    @LuaMethod
    public static int cancel(LuaState state) {
        state.checkType(1, LuaType.THREAD);
        var thread = state.toThread(1);

        // todo: try to stop thread from resuming if it is scheduled

        return 0;
    }

    private static int resume(@Nullable LuaState caller, LuaState thread) {
        return switch (thread.resume(caller, 0)) {
            // 1: OK -> exit, no need to do anything
            // 2: YIELD -> simply return the thread. One of two things happen
            //    A: the caller takes the thread and resumes it again later
            //    B: a task.wait happened, and we have already saved+scheduled the thread
            case OK, YIELD -> 1;
            // 3: ERROR -> propagate
            default -> {
                // todo doesnt really work because state may not be present...
                throw caller.error(thread.toStringRepr(-1));
            }
        };
    }
}
