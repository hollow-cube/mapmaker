package net.hollowcube.mapmaker.map.script.object;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.script.friendly.LuaObject;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.ToIntBiFunction;

public class EventSource<E extends Event> implements LuaObject {
    private static final String TYPE_NAME = EventSource.class.getName();

    public static void initGlobalLib(@NotNull LuaState state) {
        state.newMetaTable(TYPE_NAME);

        state.pushCFunction(EventSource::luaIndex, "__index");
        state.setField(-2, "__index");

        state.pushCFunction(EventSource::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");

        state.pop(1); // Pop the metatable
    }

    private static int luaIndex(@NotNull LuaState state) {
        EventSource<?> ref = (EventSource<?>) state.checkUserDataArg(1, TYPE_NAME);
        String key = state.checkStringArg(2);

        return switch (key) {
            default -> {
                state.argError(2, "No such key: " + key);
                yield 0; // Never reached
            }
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        EventSource<?> ref = (EventSource<?>) state.checkUserDataArg(1, TYPE_NAME);
        String method = state.nameCallAtom();

        return switch (method) {
            case "Listen" -> ref.listen(state);
            default -> {
                state.error("No such method: " + method);
                yield 0; // Never reached
            }
        };
    }

    private final LuaState state;

    private final EventNode<E> eventNode;
    private final Class<E> eventClass;
    private final ToIntBiFunction<LuaState, E> pushArgsFunction;
    private final int nresults;
    private final BiConsumer<LuaState, E> afterFunction;

    private final IntSet refs = new IntArraySet();

    public EventSource(
            @NotNull LuaState state,
            @NotNull EventNode<E> eventNode,
            @NotNull Class<E> eventClass) {
        this(state, eventNode, eventClass, (s, e) -> 0, 0, (s, e) -> {
        });
    }

    public EventSource(
            @NotNull LuaState state,
            @NotNull EventNode<E> eventNode,
            @NotNull Class<E> eventClass,
            @NotNull ToIntBiFunction<LuaState, E> pushArgsFunction,
            int nresults,
            @NotNull BiConsumer<LuaState, E> afterFunction
    ) {
        this.state = state;
        this.eventNode = eventNode;
        this.eventClass = eventClass;
        this.pushArgsFunction = pushArgsFunction;
        this.nresults = nresults;
        this.afterFunction = afterFunction;
    }

    public int listen(@NotNull LuaState state) {
        state.checkType(2, LuaType.FUNCTION);
        int funcRef = state.ref(2);
        state.pop(1);

        //todo dont add a listener for every single one. can just add a single listener.
        this.refs.add(funcRef);
        this.eventNode.addListener(eventClass, e -> {
            state.getref(funcRef);
            int nargs = this.pushArgsFunction.applyAsInt(state, e);
            state.pcall(nargs, nresults);
            this.afterFunction.accept(state, e);
        });

        return 0;
    }

    public void close(@NotNull LuaState state) {
        this.refs.forEach(state::unref);
    }
}
