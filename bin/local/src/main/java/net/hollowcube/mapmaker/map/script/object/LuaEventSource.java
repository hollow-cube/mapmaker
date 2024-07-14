package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.func.LuaFunctions;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.script.engine.ScriptContainer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface LuaEventSource<F> {

    static <E extends Event, F> Pin<LuaEventSource<F>> create(
            @NotNull Class<F> functionType,
            @NotNull Class<E> eventType,
            @NotNull BiConsumer<E, F> binder
    ) {
        return Pin.value(new EventImpl<>(eventType, functionType, binder));
    }

    static <F> Pin<LuaEventSource<F>> trigger(
            @NotNull Class<F> function,
            @NotNull Consumer<LuaState> create
    ) {
        return Pin.value(new TriggerImpl<>(function, create));
    }

    @LuaObject
    class EventImpl<E extends Event, F> implements LuaEventSource<F>, Pinned {
        private final Class<E> eventType;
        private final Class<F> functionType;
        private final BiConsumer<E, F> binder;

        private LuaState listenerOwningState = null;
        private EventListener<E> listener = null; // Created lazily
        private final List<Pin<F>> listeners = new ArrayList<>();

        public EventImpl(
                @NotNull Class<E> eventType,
                @NotNull Class<F> functionType,
                @NotNull BiConsumer<E, F> binder
        ) {
            this.eventType = eventType;
            this.functionType = functionType;
            this.binder = binder;
        }

        @LuaMethod
        public int listen(@NotNull LuaState state) {
            // Start at 2 because the first argument is the object itself
            listeners.add(LuaFunctions.bind(functionType, state, 2));
            if (listener == null) createListener(state);
            return 0;
        }

        private void createListener(@NotNull LuaState state) {
            this.listenerOwningState = state;
            listener = EventListener.of(eventType, e ->
                    this.listeners.forEach(f -> binder.accept(e, f.get())));
            ((ScriptContainer) state.getThreadData()).addListener(listener);
        }

        @Override
        public void unpin() {
            this.listeners.forEach(Pin::close);
            this.listeners.clear();

            if (listener != null) {
                ((ScriptContainer) listenerOwningState.getThreadData()).addListener(listener);
                this.listener = null;
                this.listenerOwningState = null;
            }
        }
    }

    @LuaObject
    class TriggerImpl<F> implements LuaEventSource<F>, Pinned {
        private final Class<F> functionType;
        private final Consumer<LuaState> create;

        private final List<Pin<F>> listeners = new ArrayList<>();

        public TriggerImpl(@NotNull Class<F> functionType, @NotNull Consumer<LuaState> create) {
            this.functionType = functionType;
            this.create = create;
        }

        @LuaMethod
        public int listen(@NotNull LuaState state) {
            // Start at 2 because the first argument is the object itself
            listeners.add(LuaFunctions.bind(functionType, state, 2));
            create.accept(state);
            return 0;
        }

        public void trigger(@NotNull Consumer<F> caller) {
            listeners.forEach(c -> caller.accept(c.get()));
        }

        @Override
        public void unpin() {
            this.listeners.forEach(Pin::close);
            this.listeners.clear();
        }
    }

}
