package net.hollowcube.mapmaker.map.scripting;

import net.hollowcube.luau.LuaState;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;

public class LuaScriptState {
    public static @NotNull LuaScriptState from(@NotNull LuaState state) {
        return switch (state.getThreadData()) {
            case LuaScriptState threadState -> threadState;
            case Holder holder -> holder.scriptState();
            case null -> throw new IllegalStateException("No thread data set for LuaState: " + state);
            default ->
                    throw new IllegalArgumentException("Invalid thread data type: " + state.getThreadData().getClass());
        };
    }

    public interface Holder {
        @NotNull LuaScriptState scriptState();
    }

    private final LuaState main; // The "main" thread for this script.
    private final Player player;
    private final int mainRef;

    private final EventNode<EntityEvent> eventNode = EventNode.type("lua-script-state", EventFilter.ENTITY);

    public LuaScriptState(@NotNull LuaState main, @NotNull Player player, int mainRef) {
        this.main = main;
        this.player = player;
        this.mainRef = mainRef;
    }

    public @NotNull Scheduler scheduler() {
        return player.scheduler();
    }

    public int mainRef() {
        return mainRef;
    }

    public EventNode<EntityEvent> eventNode() {
        return eventNode;
    }
}
