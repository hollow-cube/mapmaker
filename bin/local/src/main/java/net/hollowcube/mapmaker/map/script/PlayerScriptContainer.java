package net.hollowcube.mapmaker.map.script;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.script.object.LuaPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerScriptContainer {
    public static final Tag<PlayerScriptContainer> TAG = Tag.Transient("player_script_container");

    private final LuaState global;
    private final Player player;

    private final LuaState thread;
    private int ref; // Contains a Lua ref to the thread, so it is not collected.

    private final Pin<LuaPlayer> playerRef;
//    private final Ref<LuaPlayer> playerRef;

    private final EventNode<InstanceEvent> eventNode = EventNode.event("player-world-events", EventFilter.INSTANCE, event -> {
        if (!(event instanceof PlayerInstanceEvent pie)) return false;
        return pie.getPlayer() == player();
    });

    public PlayerScriptContainer(@NotNull LuaState global, @NotNull Player player) {
        this.global = global;
        this.player = player;

        this.thread = global.newThread();
        this.ref = global.ref(-1);
        global.pop(1); // Remove the thread from the stack (it will remain as long as ref does)

        thread.sandboxThread();
        thread.setThreadData(this);

        this.playerRef = Pin.value(new LuaPlayer(player));
        
        player.getInstance().eventNode().addChild(eventNode);
    }

    public @NotNull Player player() {
        return player;
    }

    public void eval(@NotNull String name, byte @NotNull [] bytecode) {
        thread.load(name, bytecode);
        thread.pcall(0, 0); // eval the code.
    }

    public void close() {
        Check.stateCondition(ref == 0, "Already closed");
        this.global.unref(ref);
        this.ref = 0;

        this.tasks.forEach(task -> {
            task.task.cancel();
            thread.unref(task.funcRef);
        });
        this.tasks.clear();

        this.playerRef.close();

        player.getInstance().eventNode().removeChild(eventNode);
    }

    public @NotNull Pin<LuaPlayer> getParent() {
        return playerRef;
    }

    public void addListener(EventListener<?> listener) {
        eventNode.addListener((EventListener<? extends InstanceEvent>) listener);
    }

    public void removeListener(EventListener<?> listener) {
        eventNode.removeListener((EventListener<? extends InstanceEvent>) listener);
    }

    record TaskRef(Task task, int funcRef) {

    }

    private final List<TaskRef> tasks = new ArrayList<>();

    public int schedule(@NotNull LuaState state) {
        int ticks = state.checkIntegerArg(2);
        state.checkType(3, LuaType.FUNCTION);
        int funcRef = state.ref(3);

        var taskRef = new AtomicReference<TaskRef>();
        var task = player.scheduler().buildTask(() -> {
            state.getref(funcRef);
            state.unref(funcRef);
            state.pcall(0, 0);
            this.tasks.remove(taskRef.get());
        }).delay(TaskSchedule.tick(ticks)).schedule();
        var r = new TaskRef(task, funcRef);
        taskRef.set(r);
        tasks.add(r);

        return 0;
    }


}
