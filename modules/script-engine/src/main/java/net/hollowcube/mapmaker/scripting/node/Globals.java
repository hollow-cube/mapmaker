package net.hollowcube.mapmaker.scripting.node;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.minestom.server.thread.TickThread;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class Globals {

    private static final Logger log = LoggerFactory.getLogger(Globals.class);

    public Object setTimeout(@NotNull Value... arguments) {
        if (arguments.length < 2)
            throw new IllegalArgumentException("setTimeout requires at least 2 arguments");
        if (!arguments[0].canExecute())
            throw new IllegalArgumentException("setTimeout requires a function as the first argument");
        if (!arguments[1].isNumber())
            throw new IllegalArgumentException("setTimeout requires a number as the second argument");

        final Value function = arguments[0];
        final long delay = arguments[1].asLong();
        final Value[] args = new Value[arguments.length - 2];
        System.arraycopy(arguments, 2, args, 0, args.length);

        if (args.length == 0 && delay <= 0) {
            InventoryHost.current().scheduleMicrotask(function);
            return -1;
        }

        log.error("unsupported long timeout: {}ms", delay);

        // If zero call immediately and return empty timeout id
//        if (delay <= 0) {
//            function.executeVoid((Object[]) args);
//            return -1;
//        }

//        final int taskId = nextTaskId.getAndIncrement();
//        Runnable taskImpl = () -> {
//            try {
//
//                System.out.println("run thread t" + taskId + " " + Thread.currentThread().threadId() + " " + Thread.currentThread().getName() + " " + TickThread.current());
//
//                this.engine.instance.scheduleNextTick(ignored3 -> {
//                    this.engine.guiManager().reactReconcilerInst.invokeMember("flushSyncWork");
//                    function.executeVoid((Object[]) args);
//                });
//            } finally {
//                this.pendingTasks.remove(taskId);
//            }
//        };
//
//        if (delay <= 0) {
//            this.engine.instance.scheduler().scheduleEndOfTick(taskImpl);
//        } else {
//            System.out.println("schedule thread t" + taskId + " " + Thread.currentThread().threadId() + " " + Thread.currentThread().getName() + " " + TickThread.current());
//            this.pendingTasks.put(taskId, this.engine.instance.scheduler()
//                    .buildTask(taskImpl)
//                    .delay(TaskSchedule.millis(delay))
//                    .schedule());
//        }


        return -1;
    }

    public Object clearTimeout(@NotNull Value... arguments) {
        if (arguments.length < 1)
            throw new IllegalArgumentException("clearTimeout requires at least 1 argument");
        if (!arguments[0].isNumber())
            throw new IllegalArgumentException("clearTimeout requires a number as the first argument");

        final Value idArg = arguments[0];
        if (!idArg.isNumber()) return null;
        final int id = idArg.asInt();

        System.out.println("trying to clear " + id);
//        final Task task = this.pendingTasks.remove(id);
//        if (task != null) task.cancel();

        return null;
    }
}
