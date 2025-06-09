package net.hollowcube.mapmaker.map.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaStatus;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.scripting.LuaScriptState;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public class LuaLibTask {
    private static final String NAME = "task";

    public static void init(@NotNull LuaState state) {
        state.registerLib(NAME, Map.of(
                "spawn", LuaLibTask::spawn,
                "wait", LuaLibTask::wait
        ));
        state.pop(1);
    }

    private static int spawn(@NotNull LuaState state) {
        state.checkType(1, LuaType.FUNCTION); // todo should support coroutine being passed here also

        var luaState = LuaScriptState.from(state);

        // Create a new thread
        var thread = state.newThread();
        state.xPush(thread, 1); // Push the function onto the thread

        // Begin executing the new thread
        // This abuses minestoms behavior of immediately calling the supplier when
        // using this form of scheduleTask.
        var task = new LuaTaskWrapper(luaState, thread);
        thread.setThreadData(task);
        luaState.scheduler().submitTask(task);

        return 0;
    }

    private static int wait(@NotNull LuaState state) {
        int ticks = state.checkIntegerArg(1);
        if (ticks < 0) {
            state.argError(1, "must be a non-negative");
            return 0;
        }

        if (!(state.getThreadData() instanceof LuaTaskWrapper task)) {
            state.argError(1, "must be called from a task");
            return 0;
        }

        task.waitTicks = ticks;
        return state.yield(0);
    }

    private static class LuaTaskWrapper implements Supplier<TaskSchedule>, LuaScriptState.Holder {
        private final LuaScriptState state;
        private final LuaState thread;

        private int waitTicks = -1;

        private LuaTaskWrapper(@NotNull LuaScriptState state, @NotNull LuaState thread) {
            this.state = state;
            this.thread = thread;
        }

        @Override
        public @NotNull LuaScriptState scriptState() {
            return state;
        }

        @Override
        public @NotNull TaskSchedule get() {
            var status = thread.resume(null, 0); // again handle args here
            if (status == LuaStatus.OK) {
                return TaskSchedule.stop();
            } else if (status == LuaStatus.YIELD) {
                if (waitTicks < 0) {
                    // Probably coroutine was yielded out of band
                    return TaskSchedule.stop();
                }

                int waitTicks = this.waitTicks;
                this.waitTicks = -1;
                return TaskSchedule.tick(waitTicks);
            }

            // TODO on error this should log to the user
            var error = thread.toString(-1);
            throw new IllegalStateException("Unexpected Lua status: " + status + " with error: " + error);
        }
    }
}
