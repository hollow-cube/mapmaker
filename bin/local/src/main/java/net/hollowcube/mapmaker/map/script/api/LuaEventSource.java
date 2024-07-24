package net.hollowcube.mapmaker.map.script.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.mapmaker.map.script.engine.AbstractRefManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;

@LuaObject
public abstract class LuaEventSource<F> {

    public static <E extends Event, F> LuaEventSource<F> create(
            @NotNull Class<F> functionType,
            @NotNull Class<E> eventType,
            @NotNull BiConsumer<E, F> binder
    ) {
        return new EventImpl<>(eventType, functionType, binder);
    }

//    static <F> Pin<LuaEventSource<F>> trigger(
//            @NotNull Class<F> function,
//            @NotNull Consumer<LuaState> create
//    ) {
//        return Pin.value(new TriggerImpl<>(function, create));
//    }

    @LuaMethod
    public int listen(@NotNull LuaState state) {
        throw new UnsupportedOperationException("listen");
    }

    @LuaObject
    public static class EventImpl<E extends Event, F> extends LuaEventSource<F> {
        private final Class<E> eventType;
        private final Class<F> functionType;
        private final BiConsumer<E, F> binder;

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
            var refManager = Objects.requireNonNull((AbstractRefManager) state.getThreadData());

            var func = refManager.bindFunction(functionType, state, 2);
            refManager.addListener(EventListener.of(eventType, e -> binder.accept(e, func)));

            return 0;
        }

    }

//    @LuaObject
//    class TriggerImpl<F> implements LuaEventSource<F>, Pinned {
//        private final Class<F> functionType;
//        private final Consumer<LuaState> create;
//
//        private final List<Pin<F>> listeners = new ArrayList<>();
//
//        public TriggerImpl(@NotNull Class<F> functionType, @NotNull Consumer<LuaState> create) {
//            this.functionType = functionType;
//            this.create = create;
//        }
//
//        @LuaMethod
//        public int listen(@NotNull LuaState state) {
//            // Start at 2 because the first argument is the object itself
//            listeners.add(LuaFunctions.bind(functionType, state, 2));
//            create.accept(state);
//            return 0;
//        }
//
//        public void trigger(@NotNull Consumer<F> caller) {
//            listeners.forEach(c -> caller.accept(c.get()));
//        }
//
//        @Override
//        public void unpin() {
//            this.listeners.forEach(Pin::close);
//            this.listeners.clear();
//        }
//    }

}
