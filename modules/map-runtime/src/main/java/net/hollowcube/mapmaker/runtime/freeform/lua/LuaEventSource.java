package net.hollowcube.mapmaker.runtime.freeform.lua;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

import java.util.function.ToIntBiFunction;

import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchMethod;

public class LuaEventSource<E extends Event> {
    private static final String NAME = "EventSource";

    public static void init(LuaState state) {
        state.newMetaTable(NAME);
        state.pushCFunction(LuaEventSource::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaEventSource::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(LuaState state, LuaEventSource<?> eventSource) {
        state.newUserData(eventSource);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static LuaEventSource<?> checkArg(LuaState state, int index) {
        return (LuaEventSource<?>) state.checkUserDataArg(index, NAME);
    }

    private final EventNode<? super E> eventNode;
    private final Class<E> eventType;
    private final ToIntBiFunction<LuaState, E> pushArgs;

    public LuaEventSource(EventNode<? super E> eventNode, Class<E> eventType, ToIntBiFunction<LuaState, E> pushArgs) {
        this.eventNode = eventNode;
        this.eventType = eventType;
        this.pushArgs = pushArgs;
    }

    // Methods

    private int listen(LuaState state) {
        state.checkType(1, LuaType.FUNCTION);
        int callbackRef = state.ref(1);

        eventNode.addListener(eventType, event -> {
            state.getref(callbackRef);
            int argCount = pushArgs.applyAsInt(state, event);
            state.pcall(argCount, 0);
        });

        return 0; // todo return handle to cancel the listener
    }

    // Metatable

    private static int luaIndex(LuaState state) {
        final LuaEventSource eventSource = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(LuaState state) {
        final LuaEventSource eventSource = checkArg(state, 1);
        state.remove(1); // Remove the player userdata from the stack
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            case "Listen" -> eventSource.listen(state);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }
}
