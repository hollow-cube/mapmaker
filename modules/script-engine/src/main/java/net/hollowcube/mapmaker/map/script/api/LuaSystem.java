package net.hollowcube.mapmaker.map.script.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.error.LuaArgError;
import net.hollowcube.mapmaker.map.script.AbstractRefManager;
import net.hollowcube.mapmaker.map.script.container.ScriptContainer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@LuaObject(name = "system", singleton = true)
public final class LuaSystem {

    @LuaMethod
    public static void runLater(@NotNull LuaState state, int ticks, @NotNull Callbacks.Task task) {
        if (ticks <= 0) throw new LuaArgError(0, "expected positive integer");

        var scheduler = ((ScriptContainer) state.getThreadData()).world().instance().scheduler();
        var scheduledTask = scheduler.buildTask(task::run).delay(TaskSchedule.tick(ticks)).schedule();
        ((AbstractRefManager) state.getThreadData()).addTask(scheduledTask);
    }

    public static class Callbacks {

        @LuaBindable
        public interface Task {
            void run();
        }

    }

    private LuaSystem() {
    }

}
