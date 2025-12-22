package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.api.LibBase.EventSource.EventPusher;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

@LuaLibrary(name = "@mapmaker")
public final class LibBase {

    public static <E extends EntityEvent> void pushEventSource(LuaState state, Class<E> event, EventPusher<E> pusher) {
        LibBase$luau.pushEventSource(state, new EventSource(event, pusher));
    }

    @LuaExport
    public static final class EventSource {

        @FunctionalInterface
        public interface EventPusher<E extends Event> {
            int pushArgs(LuaState state, E event);
        }

        private final Class<? extends Event> eventClass;
        private final EventPusher<? extends Event> pusher;

        private EventSource(Class<? extends Event> eventClass, EventPusher<? extends Event> pusher) {
            this.eventClass = eventClass;
            this.pusher = pusher;
        }

        @LuaMethod
        public int listen(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);

            EventHandle handle = new EventHandle();
            handle.chunkName = LuaHelpers.currentChunkName(state);
            handle.eventType = this.eventClass;
            handle.pusher = this.pusher;
            handle.state = state;
            handle.handlerRef = state.ref(1);

            state.pushThread(state);
            handle.threadRef = state.ref(-1);
            state.pop(1);

            var context = ScriptContext.get(state);
            context.eventNode().addListener(handle);
            context.track(handle);

            // TODO: return handle to cancel
            return 0;
        }

        @LuaMethod
        public int once(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);

            EventHandle handle = new EventHandle();
            handle.eventType = this.eventClass;
            handle.pusher = this.pusher;
            handle.state = state;
            handle.handlerRef = state.ref(1);
            handle.singleShot = true;

            state.pushThread(state);
            handle.threadRef = state.ref(-1);
            state.pop(1);

            var context = ScriptContext.get(state);
            context.eventNode().addListener(handle);
            context.track(handle);

            // TODO: return handle to cancel
            return 0;
        }

        @LuaMethod
        public int wait(LuaState state) {
            EventHandle handle = new EventHandle();
            handle.eventType = this.eventClass;
            handle.pusher = this.pusher;
            handle.state = state;
            handle.handlerRef = -1; // Resume on trigger
            handle.singleShot = true;

            state.pushThread(state);
            handle.threadRef = state.ref(-1);
            state.pop(1);

            var context = ScriptContext.get(state);
            context.eventNode().addListener(handle);
            context.track(handle);

            return state.yield(0);
        }

        @SuppressWarnings("NotNullFieldNotInitialized")
        private static final class EventHandle implements EventListener<Event>, Disposable {
            public String chunkName;
            public Class<? extends Event> eventType;
            public EventPusher<? extends Event> pusher;
            public LuaState state;
            public int threadRef;
            public int handlerRef = -1; // Resume on call
            public boolean singleShot = false;

            private boolean expired = false;

            @Override
            public Class<Event> eventType() {
                //noinspection unchecked
                return (Class<Event>) eventType;
            }

            @Override
            public Result run(Event event) {
                if (expired) return Result.EXPIRED;

                // If we have a handler ref then push the function for calling.
                if (handlerRef != -1) state.getRef(handlerRef);

                // Push the args from the events
                //noinspection unchecked
                int nargs = ((EventPusher<@NotNull Event>) pusher).pushArgs(state, event);
                if (nargs < 0) {
                    // Ignored the event, continue
                    state.pop(1); // remove handler
                    return Result.INVALID;
                }

                // Call or resume depending on what we have.
                if (handlerRef != -1) state.call(nargs, 0);
                else state.resume(null, nargs);

                if (singleShot) {
                    dispose();
                    return Result.EXPIRED;
                }
                return Result.SUCCESS;
            }

            @Override
            public void dispose() {
                expired = true;
                state.unref(threadRef);
                state.unref(handlerRef);
            }

            @Override
            public boolean isDisposed() {
                return expired;
            }

            @Override
            public String chunkName() {
                return chunkName;
            }

            @Override
            public boolean disposeOnReload() {
                return true;
            }
        }
    }

}
