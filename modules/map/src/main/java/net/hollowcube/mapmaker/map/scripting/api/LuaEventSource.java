package net.hollowcube.mapmaker.map.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.scripting.ScriptingFeatureProvider;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchMethod;

public class LuaEventSource<E extends EntityEvent> {
    private static final String NAME = "EventSource";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Player
        state.newMetaTable(NAME);
        state.pushCFunction(LuaEventSource::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaEventSource::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(@NotNull LuaState state, @NotNull LuaEventSource<?> eventSource) {
        state.newUserData(eventSource);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull LuaEventSource<?> checkArg(@NotNull LuaState state, int index) {
        return (LuaEventSource<?>) state.checkUserDataArg(index, NAME);
    }

    private final Player player;
    private final Class<E> eventType;

    public LuaEventSource(@NotNull Player player, @NotNull Class<E> eventType) {
        this.player = player;
        this.eventType = eventType;
    }

    // Methods

    private int listen(@NotNull LuaState state) {
        state.checkType(1, LuaType.FUNCTION);
        int ref = state.ref(1);

        var ttt = player.getTag(ScriptingFeatureProvider.LUA_THREAD_REF_TAG);
        ttt.eventNode().addListener(eventType, event -> {
            state.getref(ref);
            state.pcall(0, 0);
        });

        return 0;
    }

    // Metatable

    private static int luaIndex(@NotNull LuaState state) {
        final LuaEventSource eventSource = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final LuaEventSource eventSource = checkArg(state, 1);
        state.remove(1); // Remove the player userdata from the stack
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            case "Listen" -> eventSource.listen(state);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }
}
